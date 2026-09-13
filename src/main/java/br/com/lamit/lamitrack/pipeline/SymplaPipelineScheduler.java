package br.com.lamit.lamitrack.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Agendamento semanal automático do pipeline da Sympla (issue #45, ADR 0003):
 * o scraper não precisa de dado em tempo real, então roda como job agendado
 * uma vez por semana em vez de processo contínuo.
 *
 * <p>Coexiste com o disparo manual da #44 ({@code CommandLineRunner} em
 * {@link PipelineConfiguration}): ambos usam a mesma flag
 * {@code lamitrack.pipeline.enabled} e a mesma URL
 * ({@code lamitrack.pipeline.url}). O disparo manual roda uma vez ao subir;
 * o scheduler roda a cada ciclo do cron (default: domingo às 03:00,
 * configurável em {@code lamitrack.pipeline.cron}). Não há conflito: o
 * pipeline é idempotente (upsert por {@code registrationUrl}), então rodar
 * os dois na mesma execução só atualiza a mesma linha.
 */
public class SymplaPipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(SymplaPipelineScheduler.class);

    private final SymplaPipeline pipeline;
    private final PipelineProperties properties;

    public SymplaPipelineScheduler(SymplaPipeline pipeline, PipelineProperties properties) {
        this.pipeline = pipeline;
        this.properties = properties;
    }

    /**
     * Disparo agendado: roda o pipeline completo contra a URL configurada em
     * {@code lamitrack.pipeline.url}. Se a URL não foi definida, loga e não
     * faz nada (mesma regra do disparo manual da #44).
     */
    @Scheduled(cron = "${lamitrack.pipeline.cron:" + PipelineProperties.CRON_DEFAULT + "}")
    public void executarAgendado() {
        String url = properties.url();
        if (url == null || url.isBlank()) {
            log.warn("lamitrack.pipeline.enabled=true mas lamitrack.pipeline.url não foi definida; agendamento sem nada a fazer");
            return;
        }
        log.info("Agendamento semanal: rodando pipeline para {}", url);
        boolean persistido = pipeline.processar(url);
        log.info(persistido
                ? "Pipeline agendado: evento persistido a partir de " + url
                : "Pipeline agendado: nenhum evento persistido a partir de " + url);
    }
}
