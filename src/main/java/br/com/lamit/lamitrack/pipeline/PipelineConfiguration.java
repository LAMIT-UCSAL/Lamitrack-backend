package br.com.lamit.lamitrack.pipeline;

import br.com.lamit.lamitrack.scraper.SymplaScraper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração do pipeline da Sympla (issues #44 e #45).
 *
 * <p>Com {@code lamitrack.pipeline.enabled=true} e
 * {@code lamitrack.pipeline.url=<url>} o app:
 * <ul>
 *   <li>roda o pipeline uma vez ao subir (disparo manual, #44 — ex.
 *       {@code java -jar ... --lamitrack.pipeline.enabled=true
 *       --lamitrack.pipeline.url=https://www.sympla.com.br/evento/...});</li>
 *   <li>roda o pipeline automaticamente a cada ciclo do cron
 *       {@code lamitrack.pipeline.cron} (default: domingo às 03:00, uma vez
 *       por semana — agendamento semanal, #45 / ADR 0003).</li>
 * </ul>
 *
 * <p>Os dois modos compartilham a mesma flag {@code enabled} e a mesma URL;
 * não há conflito porque o pipeline é idempotente (upsert por
 * {@code registrationUrl}).
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PipelineProperties.class)
public class PipelineConfiguration {

    @Bean
    public SymplaScraper symplaScraper(ObjectMapper objectMapper) {
        return new SymplaScraper(objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "lamitrack.pipeline.enabled", havingValue = "true")
    public CommandLineRunner symplaPipelineRunner(SymplaPipeline pipeline,
                                                  PipelineProperties properties) {
        return args -> {
            String url = properties.url();
            if (url == null || url.isBlank()) {
                System.err.println("lamitrack.pipeline.enabled=true mas lamitrack.pipeline.url não foi definida; nada a fazer");
                return;
            }
            boolean persistido = pipeline.processar(url);
            System.out.println(persistido
                    ? "Pipeline: evento persistido a partir de " + url
                    : "Pipeline: nenhum evento persistido a partir de " + url);
        };
    }

    /**
     * Agendamento semanal automático (issue #45): só existe com
     * {@code lamitrack.pipeline.enabled=true}, a mesma flag do disparo manual
     * da #44. A expressão cron vem de {@code lamitrack.pipeline.cron}
     * (default: {@link PipelineProperties#CRON_DEFAULT}).
     */
    @Bean
    @ConditionalOnProperty(name = "lamitrack.pipeline.enabled", havingValue = "true")
    public SymplaPipelineScheduler symplaPipelineScheduler(SymplaPipeline pipeline,
                                                           PipelineProperties properties) {
        return new SymplaPipelineScheduler(pipeline, properties);
    }
}
