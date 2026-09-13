package br.com.lamit.lamitrack.whatsapp;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

/**
 * Processamento do lote de eventos novos para o WhatsApp (issue #57): busca
 * os eventos ainda não enviados ({@code whatsappSentAt IS NULL}), monta a
 * mensagem formatada, envia para o grupo via {@link EvolutionApiClient} e,
 * só com sucesso, marca {@code whatsappSentAt} com o horário atual.
 *
 * <p>Uma falha em um evento específico (ex.: HTTP 5xx da Evolution API) é
 * logada e não interrompe o processamento dos demais eventos do lote, nem
 * propaga para fora do método. Rodar o processamento duas vezes não reenvia
 * o que já foi marcado na primeira.
 *
 * <p>O agendamento automático e o limite diário de mensagens (issue #58)
 * usam {@link #processarEventosNovos(int)}, que processa no máximo
 * {@code limite} eventos por chamada — o resto fica para a próxima execução.
 */
public class WhatsappEventService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappEventService.class);

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final EventRepository eventRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final SiteProperties siteProperties;

    public WhatsappEventService(EventRepository eventRepository,
                                EvolutionApiClient evolutionApiClient,
                                SiteProperties siteProperties) {
        this.eventRepository = eventRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.siteProperties = siteProperties;
    }

    /**
     * Processa o lote completo de eventos novos
     * ({@code whatsappSentAt IS NULL}) sem limite (issue #57) — comportamento
     * original do disparo manual.
     *
     * @return quantidade de eventos enviados com sucesso
     */
    public int processarEventosNovos() {
        return processarLote(eventRepository.findByWhatsappSentAtIsNull());
    }

    /**
     * Processa no máximo {@code limite} eventos novos
     * ({@code whatsappSentAt IS NULL}) (issue #58): se houver mais eventos
     * novos do que o limite, processa só os primeiros (em ordem de id) — o
     * resto fica para a próxima execução.
     *
     * <p>Intencionalmente sem {@code @Transactional} no lote: cada
     * {@code save} já é transacional por si (Spring Data), e manter uma
     * transação aberta durante as chamadas HTTP à Evolution API seguraria
     * conexão/locks por tempo longo.
     *
     * @param limite máximo de eventos a processar nesta chamada;
     *        {@code <= 0} não processa nenhum
     * @return quantidade de eventos enviados com sucesso
     */
    public int processarEventosNovos(int limite) {
        if (limite <= 0) {
            log.info("Limite de eventos para esta execução é {}; nada a processar", limite);
            return 0;
        }
        return processarLote(eventRepository.findByWhatsappSentAtIsNullOrderByIdAsc(
                PageRequest.of(0, limite)));
    }

    /**
     * Envia cada evento do lote para o grupo: monta a mensagem, envia e, em
     * caso de sucesso, marca {@code whatsappSentAt} = agora e salva. Uma
     * falha em um evento específico é logada e não interrompe os demais.
     *
     * @return quantidade de eventos enviados com sucesso
     */
    private int processarLote(List<Event> eventos) {
        int enviados = 0;
        for (Event evento : eventos) {
            try {
                evolutionApiClient.enviarTextoParaGrupo(mensagemPara(evento));
                evento.setWhatsappSentAt(LocalDateTime.now());
                eventRepository.save(evento);
                enviados++;
                log.info("Evento {} ({}) enviado para o grupo do WhatsApp",
                        evento.getId(), evento.getTitle());
            } catch (RuntimeException e) {
                // Qualquer falha em um evento específico (HTTP 5xx da Evolution
                // API, evento malformado, etc.) é logada e não interrompe o
                // processamento dos demais eventos do lote.
                log.error("Falha ao enviar evento {} ({}) para o WhatsApp: {}",
                        evento.getId(), evento.getTitle(), e.toString());
            }
        }
        return enviados;
    }

    /**
     * Calcula quantos eventos ainda podem ser enviados hoje (issue #58):
     * {@code dailyLimit - (enviados no dia corrente)}, nunca negativo.
     *
     * @param dailyLimit limite diário configurado
     * @return restante do limite diário (0 se já atingido)
     */
    public int restanteDoLimiteDiario(int dailyLimit) {
        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = inicioDoDia.plusDays(1);
        long enviadosHoje = eventRepository.countByWhatsappSentAtBetween(inicioDoDia, fimDoDia);
        int restante = dailyLimit - (int) enviadosHoje;
        return Math.max(restante, 0);
    }

    /**
     * Mensagem formatada do evento: título, data/hora ({@code startsAt}),
     * local (venue/cidade/UF) e link para a página do evento no site
     * LamiTrack.
     */
    public String mensagemPara(Event evento) {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append(evento.getTitle()).append('\n');
        mensagem.append("Data: ").append(evento.getStartsAt().format(FORMATO_DATA_HORA)).append('\n');
        String local = localDoEvento(evento);
        if (local != null) {
            mensagem.append("Local: ").append(local).append('\n');
        }
        mensagem.append(linkDoEvento(evento));
        return mensagem.toString();
    }

    /**
     * Link para a página do evento no site: a base configurável
     * ({@code lamitrack.site.base-url}) concatenada com o id do evento — a
     * rota final do site ainda não está fixada, então nada é hardcoded.
     */
    public String linkDoEvento(Event evento) {
        return siteProperties.baseUrl() + "/eventos/" + evento.getId();
    }

    /**
     * Local do evento a partir de venue/cidade/UF, tolerando campos ausentes
     * (ex.: {@code "Teatro Jardim Sul - São Paulo/SP"}); {@code null} se o
     * evento não tem nenhum dado de local.
     */
    private String localDoEvento(Event evento) {
        List<String> partes = new ArrayList<>();
        if (evento.getVenue() != null && !evento.getVenue().isBlank()) {
            partes.add(evento.getVenue());
        }
        StringBuilder cidadeUf = new StringBuilder();
        if (evento.getCity() != null && !evento.getCity().isBlank()) {
            cidadeUf.append(evento.getCity());
        }
        if (evento.getUf() != null && !evento.getUf().isBlank()) {
            if (!cidadeUf.isEmpty()) {
                cidadeUf.append("/");
            }
            cidadeUf.append(evento.getUf());
        }
        if (!cidadeUf.isEmpty()) {
            partes.add(cidadeUf.toString());
        }
        return partes.isEmpty() ? null : String.join(" - ", partes);
    }
}
