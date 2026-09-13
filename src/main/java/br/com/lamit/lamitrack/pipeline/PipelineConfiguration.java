package br.com.lamit.lamitrack.pipeline;

import br.com.lamit.lamitrack.scraper.SymplaScraper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do pipeline da Sympla (issue #44).
 *
 * <p>Disparo manual por enquanto: com {@code lamitrack.pipeline.enabled=true}
 * e {@code lamitrack.pipeline.url=<url>} o app roda o pipeline uma vez ao
 * subir (ex. {@code mvn spring-boot:run -Dspring-boot.run.arguments=...} ou
 * {@code java -jar ... --lamitrack.pipeline.enabled=true
 * --lamitrack.pipeline.url=https://www.sympla.com.br/evento/...}). O
 * agendamento semanal automático é a issue #45.
 */
@Configuration
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
}
