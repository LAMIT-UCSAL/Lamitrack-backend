package br.com.lamit.lamitrack.whatsapp;

import br.com.lamit.lamitrack.event.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do bot de WhatsApp (issue #57), no mesmo padrão do pipeline da
 * Sympla ({@code PipelineConfiguration}): kill switch por
 * {@code @ConditionalOnProperty} — com {@code lamitrack.whatsapp.enabled}
 * ausente ou {@code false}, nenhum bean deste pacote é criado.
 *
 * <p>Com {@code lamitrack.whatsapp.enabled=true} o app:
 * <ul>
 *   <li>cria o {@link EvolutionApiClient} e o {@link WhatsappEventService};</li>
 *   <li>roda o processamento do lote de eventos novos uma vez ao subir
 *       (disparo manual — o agendamento automático e o limite diário de
 *       mensagens são da issue #58).</li>
 * </ul>
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
}
