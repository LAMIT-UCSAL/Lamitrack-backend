package br.com.lamit.lamitrack.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Agendamento diário automático do processamento do lote de eventos novos do
 * WhatsApp (issue #58): o bot não precisa de tempo real, então roda como job
 * agendado uma vez por dia em vez de processo contínuo (mesmo estilo do
 * agendamento semanal do pipeline, issue #45 / ADR 0003).
 *
 * <p>Coexiste com o disparo manual da #57 ({@code CommandLineRunner} em
 * {@link WhatsappConfiguration}): ambos usam a mesma flag
 * {@code lamitrack.whatsapp.enabled}. O disparo manual roda uma vez ao subir
 * sem limite; o scheduler roda a cada ciclo do cron
 * ({@code lamitrack.whatsapp.cron}, default: todos os dias às 09:00)
 * respeitando o limite diário ({@code lamitrack.whatsapp.daily-limit},
 * default: 20) — cada execução processa no máximo o que resta do limite do
 * dia, considerando os eventos já enviados hoje
 * ({@code whatsappSentAt} no intervalo do dia corrente). O cron é
 * independente do cron do scraper ({@code lamitrack.pipeline.cron}).
 */
public class WhatsappEventScheduler {

    private static final Logger log = LoggerFactory.getLogger(WhatsappEventScheduler.class);

    private final WhatsappEventService whatsappEventService;
    private final WhatsappProperties properties;

    public WhatsappEventScheduler(WhatsappEventService whatsappEventService,
                                  WhatsappProperties properties) {
        this.whatsappEventService = whatsappEventService;
        this.properties = properties;
    }

    /**
     * Disparo agendado: processa no máximo o restante do limite diário de
     * eventos novos. Passar do limite não falha a execução — só limita o
     * processamento desta rodada; o resto fica para a próxima.
     */
    @Scheduled(cron = "${lamitrack.whatsapp.cron:" + WhatsappProperties.CRON_DEFAULT + "}")
    public void executarAgendado() {
        int limiteDiario = properties.dailyLimitEfetivo();
        int restante = whatsappEventService.restanteDoLimiteDiario(limiteDiario);
        if (restante == 0) {
            log.info("Limite diário de {} evento(s) já atingido; agendamento sem nada a fazer",
                    limiteDiario);
            return;
        }
        log.info("Agendamento diário: processando no máximo {} evento(s) novo(s) "
                + "(restante do limite diário de {})", restante, limiteDiario);
        int enviados = whatsappEventService.processarEventosNovos(restante);
        log.info("Agendamento diário: {} evento(s) enviado(s) para o grupo do WhatsApp",
                enviados);
    }
}
