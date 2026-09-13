package br.com.lamit.lamitrack.whatsapp;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>O agendamento automático e o limite diário de mensagens são da issue
 * #58 — este ticket expõe apenas o disparo manual
 * ({@link #processarEventosNovos()} e o {@code CommandLineRunner} condicional
 * em {@link WhatsappConfiguration}).
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
     * Processa o lote de eventos novos ({@code whatsappSentAt IS NULL}): para
     * cada evento, monta a mensagem, envia para o grupo e, em caso de
     * sucesso, marca {@code whatsappSentAt} = agora e salva.
     *
     * <p>Intencionalmente sem {@code @Transactional} no lote: cada
     * {@code save} já é transacional por si (Spring Data), e manter uma
     * transação aberta durante as chamadas HTTP à Evolution API seguraria
     * conexão/locks por tempo longo.
     *
     * @return quantidade de eventos enviados com sucesso
     */
    public int processarEventosNovos() {
        List<Event> eventosNovos = eventRepository.findByWhatsappSentAtIsNull();
        int enviados = 0;
        for (Event evento : eventosNovos) {
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
