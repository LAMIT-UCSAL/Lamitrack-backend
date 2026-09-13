package br.com.lamit.lamitrack.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Integração do agendamento semanal (issue #45 / ADR 0003): prova que
 * disparar o método agendado manualmente (chamar
 * {@link SymplaPipelineScheduler#executarAgendado()} diretamente, sem esperar
 * o cron) aciona o pipeline completo — HTTP real (contra um servidor local
 * que serve a fixture HTML da #43/#44, sem rede externa), parse e
 * persistência no Postgres real via Testcontainers (mesmo padrão do
 * {@code SymplaPipelineIntegrationTest}).
 */
@Testcontainers
@SpringBootTest
@Transactional
class SymplaPipelineSchedulerIntegrationTest {

    private static final String URL_FIXTURE =
            "https://www.sympla.com.br/evento/abba-world-tribute/3280023";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lamitrack")
            .withUsername("lamitrack")
            .withPassword("lamitrack");

    /** Servidor local que serve a fixture HTML da #43/#44, para o pipeline
     *  fazer HTTP real sem depender da rede externa. */
    static HttpServer httpServer;
    static int porta;
    static byte[] fixtureHtml;
    static final AtomicInteger requisicoes = new AtomicInteger();

    static {
        try {
            fixtureHtml = Files.readString(
                            Path.of("src/test/resources/sympla-evento-abba-world-tribute.html"),
                            StandardCharsets.UTF_8)
                    .getBytes(StandardCharsets.UTF_8);
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/evento", exchange -> {
                requisicoes.incrementAndGet();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, fixtureHtml.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fixtureHtml);
                }
            });
            httpServer.start();
            porta = httpServer.getAddress().getPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("lamitrack.pipeline.enabled", () -> "true");
        registry.add("lamitrack.pipeline.url", () -> "http://127.0.0.1:" + porta + "/evento");
    }

    @Autowired
    SymplaPipelineScheduler scheduler;

    @Autowired
    EventRepository eventRepository;

    @Test
    void disparoManualDoMetodoAgendadoAcionaPipelineCompletoEPersisteEvento() {
        int requisicoesAntes = requisicoes.get();

        scheduler.executarAgendado();

        // Meu disparo fez o pipeline buscar o HTML via HTTP (servidor local)
        assertThat(requisicoes.get()).isGreaterThan(requisicoesAntes);

        // ...e persistiu o evento no Postgres real
        List<Event> eventos = eventRepository.findAll();
        assertThat(eventos).hasSize(1);
        Event salvo = eventos.get(0);
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getTitle()).isEqualTo("ABBA World Tribute");
        assertThat(salvo.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 9, 26, 20, 0));
        assertThat(salvo.getVenue()).isEqualTo("Teatro Jardim Sul");
        assertThat(salvo.getCity()).isEqualTo("São Paulo");
        assertThat(salvo.getUf()).isEqualTo("SP");
        assertThat(salvo.getRegistrationUrl()).isEqualTo(URL_FIXTURE);
        assertThat(salvo.getSource()).isEqualTo("sympla");
        assertThat(salvo.getTags()).containsExactly("musica");
    }
}
