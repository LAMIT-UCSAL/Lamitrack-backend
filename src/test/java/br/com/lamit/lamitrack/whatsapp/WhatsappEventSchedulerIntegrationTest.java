package br.com.lamit.lamitrack.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
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
 * Integração do limite diário do agendamento do WhatsApp (issue #58): eventos
 * semeados via {@link EventRepository} num Postgres real (Testcontainers,
 * mesmo padrão do {@code WhatsappEventServiceIntegrationTest}), envio real
 * via HTTP contra um servidor local que simula a Evolution API — sem rede
 * real.
 *
 * <p>Prova que: (a) com mais eventos novos do que o limite diário, uma
 * execução processa só até o limite — o resto fica para a próxima execução;
 * (b) a segunda execução processa o restante (o limite diário considera os
 * eventos já enviados HOJE via {@code whatsappSentAt}); (c) com o limite
 * diário já atingido, o agendamento não envia nada e não falha.
 */
@Testcontainers
@SpringBootTest(properties = {
        "lamitrack.whatsapp.daily-limit=2"
})
@Transactional
class WhatsappEventSchedulerIntegrationTest {

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

    @BeforeAll
    static void sobeServidorSimulado() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/", exchange -> {
                byte[] corpo = exchange.getRequestBody().readAllBytes();
                corpos.add(new String(corpo, StandardCharsets.UTF_8));
                byte[] resposta = "{\"key\":{\"id\":\"MSG-1\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resposta.length);
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
    WhatsappEventScheduler scheduler;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    WhatsappProperties properties;

    @Test
    void maisEventosNovosQueOLimiteProcessaSóAtéOLimite() {
        // 4 eventos novos, limite diário = 2
        for (int i = 1; i <= 4; i++) {
            eventRepository.save(novoEvento("Evento " + i,
                    LocalDateTime.of(2026, 10, i, 19, 0), "Local " + i, "Recife", "PE"));
        }

        // O agendamento processa no máximo o restante do limite diário (2),
        // não os 4 eventos novos
        scheduler.executarAgendado();

        // A execução processou só até o limite diário (2), não os 4
        assertThat(corpos).hasSize(2);
        // Os 2 restantes continuam "novos" (whatsappSentAt = null)
        assertThat(eventRepository.findByWhatsappSentAtIsNull()).hasSize(2);
    }

    @Test
    void segundaExecucaoProcessaORestanteDoLimiteDiario() {
        for (int i = 1; i <= 4; i++) {
            eventRepository.save(novoEvento("Evento " + i,
                    LocalDateTime.of(2026, 10, i, 19, 0), "Local " + i, "Recife", "PE"));
        }

        int primeira = whatsappEventService.processarEventosNovos(
                whatsappEventService.restanteDoLimiteDiario(properties.dailyLimitEfetivo()));
        int segunda = whatsappEventService.processarEventosNovos(
                whatsappEventService.restanteDoLimiteDiario(properties.dailyLimitEfetivo()));

        // Primeira execução: 2 (limite diário); segunda: 0 (limite já atingido
        // hoje — os 2 restantes ficam para o próximo dia)
        assertThat(primeira).isEqualTo(2);
        assertThat(segunda).isZero();
        assertThat(corpos).hasSize(2);
        assertThat(eventRepository.findByWhatsappSentAtIsNull()).hasSize(2);
    }

    @Test
    void limiteDiarioJaAtingidoNaoEnviaNadaENaoFalha() {
        // 2 eventos já enviados HOJE (whatsappSentAt = agora) atingem o limite
        for (int i = 1; i <= 2; i++) {
            Event evento = novoEvento("Evento Enviado " + i,
                    LocalDateTime.of(2026, 10, i, 19, 0), "Local " + i, "Recife", "PE");
            evento.setWhatsappSentAt(LocalDateTime.now());
            eventRepository.save(evento);
        }
        eventRepository.save(novoEvento("Evento Novo",
                LocalDateTime.of(2026, 10, 5, 19, 0), "Local 5", "Recife", "PE"));

        assertThat(whatsappEventService.restanteDoLimiteDiario(properties.dailyLimitEfetivo()))
                .isZero();

        // O agendamento não falha e não envia nada
        int enviados = whatsappEventService.processarEventosNovos(
                whatsappEventService.restanteDoLimiteDiario(properties.dailyLimitEfetivo()));
        assertThat(enviados).isZero();
        assertThat(corpos).isEmpty();
        assertThat(eventRepository.findByWhatsappSentAtIsNull()).hasSize(1);
    }

    @Test
    void eventosEnviadosOntemNaoContamNoLimiteDeHoje() {
        // 2 eventos enviados ONTEM não contam para o limite de hoje
        for (int i = 1; i <= 2; i++) {
            Event evento = novoEvento("Evento Ontem " + i,
                    LocalDateTime.of(2026, 10, i, 19, 0), "Local " + i, "Recife", "PE");
            evento.setWhatsappSentAt(LocalDateTime.now().minusDays(1));
            eventRepository.save(evento);
        }
        eventRepository.save(novoEvento("Evento Hoje",
                LocalDateTime.of(2026, 10, 5, 19, 0), "Local 5", "Recife", "PE"));

        // O limite de hoje segue inteiro (2), apesar dos 2 de ontem
        assertThat(whatsappEventService.restanteDoLimiteDiario(properties.dailyLimitEfetivo()))
                .isEqualTo(2);
    }

    private static Event novoEvento(String titulo, LocalDateTime startsAt,
                                    String venue, String city, String uf) {
        return new Event(titulo, "descricao", startsAt, venue, city, uf,
                null, null, null, "sympla", null, Set.of());
    }
}
