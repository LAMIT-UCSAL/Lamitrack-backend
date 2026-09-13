package br.com.lamit.lamitrack.whatsapp;

import br.com.lamit.lamitrack.event.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do bot de WhatsApp (issues #57 e #58), no mesmo padrão do
 * pipeline da Sympla ({@code PipelineConfiguration}): kill switch por
 * {@code @ConditionalOnProperty} — com {@code lamitrack.whatsapp.enabled}
 * ausente ou {@code false}, nenhum bean deste pacote é criado.
 *
 * <p>Com {@code lamitrack.whatsapp.enabled=true} o app:
 * <ul>
 *   <li>cria o {@link EvolutionApiClient} e o {@link WhatsappEventService};</li>
 *   <li>roda o processamento do lote de eventos novos uma vez ao subir
 *       (disparo manual, #57 — sem limite diário);</li>
 *   <li>roda o processamento automaticamente a cada ciclo do cron
 *       {@code lamitrack.whatsapp.cron} (default: todos os dias às 09:00),
 *       respeitando o limite diário
 *       {@code lamitrack.whatsapp.daily-limit} (default: 20) — agendamento
 *       diário, #58.</li>
 * </ul>
 *
 * <p>O cron do WhatsApp é independente do cron do scraper
 * ({@code lamitrack.pipeline.cron}, #45): propriedades e beans diferentes.
 */
@Configuration
@EnableConfigurationProperties({WhatsappProperties.class, SiteProperties.class})
public class WhatsappConfiguration {

    @Bean
    @ConditionalOnProperty(name = "lamitrack.whatsapp.enabled", havingValue = "true")
    public EvolutionApiClient evolutionApiClient(ObjectMapper objectMapper,
                                                 WhatsappProperties properties) {
        return new EvolutionApiClient(objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "lamitrack.whatsapp.enabled", havingValue = "true")
    public WhatsappEventService whatsappEventService(EventRepository eventRepository,
                                                     EvolutionApiClient evolutionApiClient,
                                                     SiteProperties siteProperties) {
        return new WhatsappEventService(eventRepository, evolutionApiClient, siteProperties);
    }

    /**
     * Disparo manual do processamento do lote ao subir (issue #57): envia os
     * eventos novos ({@code whatsappSentAt IS NULL}) para o grupo do
     * WhatsApp. Idempotente — eventos já marcados não são reenviados.
     */
    @Bean
    @ConditionalOnProperty(name = "lamitrack.whatsapp.enabled", havingValue = "true")
    public CommandLineRunner whatsappEventRunner(WhatsappEventService whatsappEventService) {
        return args -> {
            int enviados = whatsappEventService.processarEventosNovos();
            System.out.println("WhatsApp: " + enviados + " evento(s) novo(s) enviado(s) para o grupo");
        };
    }

    /**
     * Agendamento diário automático (issue #58): só existe com
     * {@code lamitrack.whatsapp.enabled=true}, a mesma flag do disparo manual
     * da #57. A expressão cron vem de {@code lamitrack.whatsapp.cron}
     * (default: {@link WhatsappProperties#CRON_DEFAULT}) e o limite diário de
     * eventos vem de {@code lamitrack.whatsapp.daily-limit} (default:
     * {@link WhatsappProperties#DAILY_LIMIT_DEFAULT}).
     */
    @Bean
    @ConditionalOnProperty(name = "lamitrack.whatsapp.enabled", havingValue = "true")
    public WhatsappEventScheduler whatsappEventScheduler(WhatsappEventService whatsappEventService,
                                                         WhatsappProperties properties) {
        return new WhatsappEventScheduler(whatsappEventService, properties);
    }
}
