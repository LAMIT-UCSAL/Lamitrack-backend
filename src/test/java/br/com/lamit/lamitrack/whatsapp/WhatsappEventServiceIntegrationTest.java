package br.com.lamit.lamitrack.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração fim a fim do bot de WhatsApp (issue #57): eventos semeados via
 * {@link EventRepository} num Postgres real (Testcontainers, mesmo padrão do
 * {@code EventRepositoryIntegrationTest} / {@code SymplaPipelineIntegrationTest}),
 * disparo do processamento do lote e envio real via HTTP contra um servidor
 * local que simula a Evolution API (mesmo padrão
 * {@code com.sun.net.httpserver.HttpServer} do
 * {@code SymplaPipelineSchedulerIntegrationTest}) — sem rede real.
 *
 * <p>Prova que: (a) eventos novos recebem {@code whatsappSentAt} e a mensagem
 * sai para o servidor local; (b) rodar o processamento duas vezes não reenvia
 * o que já foi marcado; (c) uma falha (HTTP 5xx) em um evento não impede o
 * processamento dos demais do lote.
 */
@Testcontainers
@SpringBootTest
@Transactional
class WhatsappEventServiceIntegrationTest {

    private static final String API_KEY = "token-da-instancia";
    private static final String GROUP_ID = "120363999999999999@g.us";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lamitrack")
            .withUsername("lamitrack")
            .withPassword("lamitrack");

    static HttpServer httpServer;
    static int porta;
    static final List<String> corpos = new CopyOnWriteArrayList<>();
    static volatile String apikeyRecebido;

    @BeforeAll
    static void sobeServidorSimulado() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/", exchange -> {
                apikeyRecebido = exchange.getRequestHeaders().getFirst("apikey");
                byte[] corpo = exchange.getRequestBody().readAllBytes();
                String texto = new String(corpo, StandardCharsets.UTF_8);
                corpos.add(texto);
                byte[] resposta = "{\"key\":{\"id\":\"MSG-1\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                // Simula falha (HTTP 500) só para a mensagem do evento com "FALHA"
                int status = texto.contains("FALHA") ? 500 : 200;
                exchange.sendResponseHeaders(status, resposta.length);
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
        corpos.clear();
        apikeyRecebido = null;
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("lamitrack.whatsapp.enabled", () -> "true");
        registry.add("lamitrack.whatsapp.base-url", () -> "http://127.0.0.1:" + porta);
        registry.add("lamitrack.whatsapp.api-key", () -> API_KEY);
        registry.add("lamitrack.whatsapp.group-id", () -> GROUP_ID);
        registry.add("lamitrack.site.base-url", () -> "https://lamint.com.br/lamitrack");
    }

    @Autowired
    WhatsappEventService whatsappEventService;

    @Autowired
    EventRepository eventRepository;

    @Test
    void eventosNovosRecebemWhatsappSentAtEMensagemEhEnviada() throws Exception {
        Event abba = eventRepository.save(novoEvento("ABBA World Tribute",
                LocalDateTime.of(2026, 9, 26, 20, 0), "Teatro Jardim Sul", "São Paulo", "SP"));
        Event churrasco = eventRepository.save(novoEvento("Churrasco Carvão 2026",
                LocalDateTime.of(2026, 11, 20, 18, 0), "Parque de Exposições", "Aracaju", "SE"));

        int enviados = whatsappEventService.processarEventosNovos();

        assertThat(enviados).isEqualTo(2);
        assertThat(eventRepository.findById(abba.getId()).orElseThrow().getWhatsappSentAt()).isNotNull();
        assertThat(eventRepository.findById(churrasco.getId()).orElseThrow().getWhatsappSentAt()).isNotNull();

        // A mensagem saiu para o servidor local simulando a Evolution API
        assertThat(corpos).hasSize(2);
        assertThat(apikeyRecebido).isEqualTo(API_KEY);
        assertThat(corpos).anySatisfy(corpo -> {
            JsonNode json = new ObjectMapper().readTree(corpo);
            assertThat(json.get("number").asText()).isEqualTo(GROUP_ID);
            assertThat(json.get("text").asText())
                    .contains("ABBA World Tribute")
                    .contains("26/09/2026 às 20:00")
                    .contains("Teatro Jardim Sul - São Paulo/SP")
                    .contains("https://lamint.com.br/lamitrack/eventos/" + abba.getId());
        });
        assertThat(corpos).anySatisfy(corpo -> {
            JsonNode json = new ObjectMapper().readTree(corpo);
            assertThat(json.get("number").asText()).isEqualTo(GROUP_ID);
            assertThat(json.get("text").asText())
                    .contains("Churrasco Carvão 2026")
                    .contains("20/11/2026 às 18:00")
                    .contains("Parque de Exposições - Aracaju/SE")
                    .contains("https://lamint.com.br/lamitrack/eventos/" + churrasco.getId());
        });
    }

    @Test
    void rodarProcessamentoDuasVezesNaoReenviaOQueJaFoiMarcado() {
        eventRepository.save(novoEvento("Evento Único",
                LocalDateTime.of(2026, 10, 1, 19, 0), "Arena", "Recife", "PE"));

        int primeira = whatsappEventService.processarEventosNovos();
        eventRepository.flush();
        int segunda = whatsappEventService.processarEventosNovos();

        assertThat(primeira).isEqualTo(1);
        assertThat(segunda).isZero();
        // Só UMA chamada HTTP saiu no total (a segunda rodada não reenviou)
        assertThat(corpos).hasSize(1);
    }

    @Test
    void falhaEmUmEventoNaoImpedeOsDemaisDoLote() {
        // O servidor local devolve 500 só para a mensagem do evento com "FALHA"
        Event falha = eventRepository.save(novoEvento("Evento FALHA",
                LocalDateTime.of(2026, 10, 2, 20, 0), "Local A", "Salvador", "BA"));
        Event ok1 = eventRepository.save(novoEvento("Evento OK 1",
                LocalDateTime.of(2026, 10, 3, 20, 0), "Local B", "Fortaleza", "CE"));
        Event ok2 = eventRepository.save(novoEvento("Evento OK 2",
                LocalDateTime.of(2026, 10, 4, 20, 0), "Local C", "Maceió", "AL"));

        int enviados = whatsappEventService.processarEventosNovos();

        // Os dois eventos OK foram enviados; o que falhou não
        assertThat(enviados).isEqualTo(2);
        assertThat(eventRepository.findById(ok1.getId()).orElseThrow().getWhatsappSentAt()).isNotNull();
        assertThat(eventRepository.findById(ok2.getId()).orElseThrow().getWhatsappSentAt()).isNotNull();
        // O evento com falha continua "novo" (whatsappSentAt = null)
        assertThat(eventRepository.findById(falha.getId()).orElseThrow().getWhatsappSentAt()).isNull();
        // As três chamadas saíram (a falha foi tentada e logada, não derrubou o lote)
        assertThat(corpos).hasSize(3);
        assertThat(corpos).anySatisfy(c -> assertThat(c).contains("Evento FALHA"));
        assertThat(corpos).anySatisfy(c -> assertThat(c).contains("Evento OK 1"));
        assertThat(corpos).anySatisfy(c -> assertThat(c).contains("Evento OK 2"));
    }

    private static Event novoEvento(String titulo, LocalDateTime startsAt,
                                    String venue, String city, String uf) {
        return new Event(titulo, "descricao", startsAt, venue, city, uf,
                null, null, null, "sympla", null, Set.of());
    }
}
