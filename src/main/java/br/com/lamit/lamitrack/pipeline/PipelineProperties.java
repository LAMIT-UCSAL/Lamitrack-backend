package br.com.lamit.lamitrack.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do pipeline da Sympla (issue #44), ligadas a
 * {@code lamitrack.pipeline.*} no application.yml / argumentos da linha de
 * comando.
 *
 * @param enabled  ativa o disparo manual do pipeline ao subir a aplicação
 * @param url      URL da página de evento da Sympla a raspar
 * @param cron     expressão cron (formato Spring) do agendamento semanal
 *                 automático (issue #45); {@code null} usa o default semanal
 */
@ConfigurationProperties(prefix = "lamitrack.pipeline")
public record PipelineProperties(Boolean enabled, String url, String cron) {

    /**
     * Default do agendamento semanal automático (issue #45 / ADR 0003):
     * domingo às 03:00, uma vez por semana.
     */
    public static final String CRON_DEFAULT = "0 0 3 ? * SUN";

    /**
     * Expressão cron efetiva: a configurada em {@code lamitrack.pipeline.cron}
     * ou, se ausente, o default semanal.
     */
    public String cronEfetivo() {
        return (cron == null || cron.isBlank()) ? CRON_DEFAULT : cron;
    }
}
