package br.com.lamit.lamitrack.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do bot de WhatsApp (issue #57), ligadas a
 * {@code lamitrack.whatsapp.*} no application.yml / argumentos da linha de
 * comando.
 *
 * @param enabled    kill switch: com {@code false} (ou ausente) nenhum bean do
 *                   pacote {@code whatsapp} é criado (mesmo padrão
 *                   {@code @ConditionalOnProperty} do pipeline, ver
 *                   {@code PipelineConfiguration})
 * @param baseUrl    URL base da instância self-hosted da Evolution API
 *                   (ex.: {@code http://evolution:8080})
 * @param apiKey     token da instância da Evolution API (header {@code apikey})
 * @param groupId    JID do grupo do WhatsApp de destino
 *                   (ex.: {@code 120363...@g.us})
 * @param instance   nome da instância da Evolution API usada na URL
 *                   {@code /message/sendText/{instance}}; {@code null} usa o
 *                   default da Evolution API
 * @param cron       expressão cron (formato Spring) do agendamento diário
 *                   automático (issue #58); {@code null} usa o default diário
 * @param dailyLimit limite máximo de eventos enviados por dia (issue #58);
 *                   {@code null} usa o default
 */
@ConfigurationProperties(prefix = "lamitrack.whatsapp")
public record WhatsappProperties(Boolean enabled, String baseUrl, String apiKey, String groupId,
                                 String instance, String cron, Integer dailyLimit) {

    /**
     * Nome default da instância da Evolution API quando
     * {@code lamitrack.whatsapp.instance} não é definido.
     */
    public static final String INSTANCE_DEFAULT = "default";

    /**
     * Default do agendamento diário automático (issue #58): todos os dias às
     * 09:00.
     */
    public static final String CRON_DEFAULT = "0 0 9 * * ?";

    /**
     * Default do limite diário de eventos enviados (issue #58).
     */
    public static final int DAILY_LIMIT_DEFAULT = 20;

    /**
     * Nome de instância efetivo: o configurado em
     * {@code lamitrack.whatsapp.instance} ou, se ausente, o default.
     */
    public String instanceEfetivo() {
        return (instance == null || instance.isBlank()) ? INSTANCE_DEFAULT : instance;
    }

    /**
     * Expressão cron efetiva: a configurada em {@code lamitrack.whatsapp.cron}
     * ou, se ausente, o default diário.
     */
    public String cronEfetivo() {
        return (cron == null || cron.isBlank()) ? CRON_DEFAULT : cron;
    }

    /**
     * Limite diário efetivo: o configurado em
     * {@code lamitrack.whatsapp.daily-limit} ou, se ausente, o default.
     */
    public int dailyLimitEfetivo() {
        return (dailyLimit == null || dailyLimit <= 0) ? DAILY_LIMIT_DEFAULT : dailyLimit;
    }
}
