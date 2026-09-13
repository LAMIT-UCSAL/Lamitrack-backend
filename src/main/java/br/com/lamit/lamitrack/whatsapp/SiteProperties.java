package br.com.lamit.lamitrack.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do site LamiTrack (issue #57), ligadas a
 * {@code lamitrack.site.*}.
 *
 * @param baseUrl URL base do site (ex.: {@code https://lamint.com.br/lamitrack}),
 *                usada para montar o link da página do evento na mensagem do
 *                WhatsApp. A rota final do site ainda não está fixada, então a
 *                base é configurável e nunca hardcoded.
 */
@ConfigurationProperties(prefix = "lamitrack.site")
public record SiteProperties(String baseUrl) {
}
