package br.com.lamit.lamitrack.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do pipeline da Sympla (issue #44), ligadas a
 * {@code lamitrack.pipeline.*} no application.yml / argumentos da linha de
 * comando.
 *
 * @param enabled ativa o disparo manual do pipeline ao subir a aplicação
 * @param url     URL da página de evento da Sympla a raspar
 */
@ConfigurationProperties(prefix = "lamitrack.pipeline")
public record PipelineProperties(Boolean enabled, String url) {
}
