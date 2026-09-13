package br.com.lamit.lamitrack.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client HTTP da Evolution API (issue #57, decisão #30): envia mensagem de
 * texto para o grupo do WhatsApp via a REST API self-hosted
 * ({@code POST /message/sendText/{instance}}), com o token da instância no
 * header {@code apikey} e o JID do grupo no campo {@code number} do corpo
 * JSON.
 *
 * <p>Toda falha (status não-2xx, rede, montagem do corpo) vira
 * {@link EvolutionApiException}, para o {@link WhatsappEventService} tratar
 * evento a evento sem derrubar o lote.
 */
public class EvolutionApiClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WhatsappProperties properties;

    public EvolutionApiClient(ObjectMapper objectMapper, WhatsappProperties properties) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Envia uma mensagem de texto para o grupo configurado em
     * {@code lamitrack.whatsapp.group-id}.
     *
     * @param texto mensagem formatada a ser enviada
     * @throws EvolutionApiException em status HTTP não-2xx, falha de rede ou
     *         falha ao montar o corpo JSON
     */
    public void enviarTextoParaGrupo(String texto) {
        String url = urlDoEnvio();
        String json;
        try {
            json = objectMapper.writeValueAsString(corpoDoEnvio(texto));
        } catch (JsonProcessingException e) {
            throw new EvolutionApiException("Falha ao montar o JSON do envio para " + url, e);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("apikey", properties.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> resposta =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() / 100 != 2) {
                throw new EvolutionApiException(
                        "HTTP " + resposta.statusCode() + " ao enviar mensagem para o grupo via " + url);
            }
            log.debug("Mensagem enviada para o grupo do WhatsApp (HTTP {})", resposta.statusCode());
        } catch (IOException e) {
            throw new EvolutionApiException("Falha de rede ao enviar mensagem para " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EvolutionApiException("Envio de mensagem para " + url + " interrompido", e);
        }
    }

    /**
     * Corpo JSON do {@code POST /message/sendText/{instance}}: o JID do grupo
     * em {@code number} e a mensagem em {@code text}.
     */
    private Map<String, Object> corpoDoEnvio(String texto) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("number", properties.groupId());
        corpo.put("text", texto);
        return corpo;
    }

    private String urlDoEnvio() {
        String base = properties.baseUrl();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/message/sendText/" + properties.instanceEfetivo();
    }
}
