package br.com.lamit.lamitrack.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Teste do client HTTP da Evolution API (issue #57) contra um servidor local
 * que simula as respostas da Evolution API (mesmo padrão
 * {@code com.sun.net.httpserver.HttpServer} do
 * {@code SymplaPipelineSchedulerIntegrationTest}) — sem rede real.
 *
 * <p>Prova que a chamada sai como {@code POST /message/sendText/{instance}}
 * com o token no header {@code apikey} e o JID do grupo + o texto da
 * mensagem no corpo JSON; e que status não-2xx vira
 * {@link EvolutionApiException}.
 */
class EvolutionApiClientTest {

    private static final String BASE_URL = "http://127.0.0.1:";
    private static final String API_KEY = "token-da-instancia";
    private static final String GROUP_ID = "120363999999999999@g.us";

    static HttpServer httpServer;
    static int porta;
    static final List<String> corpos = new java.util.concurrent.CopyOnWriteArrayList<>();
    static volatile String apikeyRecebido;
    static volatile String caminhoRecebido;
    static volatile int statusResposta = 200;

    @BeforeAll
    static void sobeServidorSimulado() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/", exchange -> {
                caminhoRecebido = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
                apikeyRecebido = exchange.getRequestHeaders().getFirst("apikey");
                byte[] corpo = exchange.getRequestBody().readAllBytes();
                corpos.add(new String(corpo, StandardCharsets.UTF_8));
                byte[] resposta = "{\"key\":{\"id\":\"MSG-1\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusResposta, resposta.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resposta);
                }
            });
            httpServer.start();
            porta = httpServer.getAddress().getPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void derrubaServidorSimulado() {
        httpServer.stop(0);
    }

    @BeforeEach
    void limpaEstadoDoServidor() {
        statusResposta = 200;
        corpos.clear();
        caminhoRecebido = null;
        apikeyRecebido = null;
    }

    @Test
    void enviaTextoParaGrupoComUrlHeaderECorpoCorretos() throws Exception {
        EvolutionApiClient client = clientComBaseUrl(BASE_URL + porta);

        client.enviarTextoParaGrupo("Evento X\nData: 20/11/2026 às 18:00\nLocal: Parque - Aracaju/SE\nhttps://lamint.com.br/lamitrack/eventos/42");

        assertThat(caminhoRecebido).isEqualTo("POST /message/sendText/default");
        assertThat(apikeyRecebido).isEqualTo(API_KEY);
        assertThat(corpos).hasSize(1);
        JsonNode json = new ObjectMapper().readTree(corpos.get(0));
        assertThat(json.get("number").asText()).isEqualTo(GROUP_ID);
        assertThat(json.get("text").asText())
                .contains("Evento X")
                .contains("Data: 20/11/2026 às 18:00")
                .contains("Local: Parque - Aracaju/SE")
                .contains("https://lamint.com.br/lamitrack/eventos/42");
    }

    @Test
    void usaInstanciaConfiguradaNaUrl() throws Exception {
        EvolutionApiClient client = new EvolutionApiClient(new ObjectMapper(),
                new WhatsappProperties(true, BASE_URL + porta, API_KEY, GROUP_ID, "lamit", null, null));

        client.enviarTextoParaGrupo("mensagem");

        assertThat(caminhoRecebido).isEqualTo("POST /message/sendText/lamit");
    }

    @Test
    void status5xxLancaEvolutionApiException() {
        statusResposta = 500;
        EvolutionApiClient client = clientComBaseUrl(BASE_URL + porta);

        assertThatThrownBy(() -> client.enviarTextoParaGrupo("mensagem"))
                .isInstanceOf(EvolutionApiException.class)
                .hasMessageContaining("HTTP 500");
    }

    private static EvolutionApiClient clientComBaseUrl(String baseUrl) {
        return new EvolutionApiClient(new ObjectMapper(),
                new WhatsappProperties(true, baseUrl, API_KEY, GROUP_ID, null, null, null));
    }
}
