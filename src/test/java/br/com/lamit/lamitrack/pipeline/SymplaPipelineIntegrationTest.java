package br.com.lamit.lamitrack.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração do pipeline fim a fim da Sympla (#44): o {@link SymplaPipeline}
 * roda o {@code SymplaScraper} (#43) contra a fixture HTML real já existente
 * (sem chamada de rede) e persiste o evento no Postgres real via
 * {@link EventRepository} (#42) com Testcontainers — mesmo padrão do
 * {@code EventRepositoryIntegrationTest}.
 *
 * <p>Prova a idempotência: rodar o pipeline duas vezes contra a mesma fixture
 * resulta em UMA linha só na tabela {@code events} (upsert por
 * {@code registrationUrl}), não duas.
 */
@Testcontainers
@SpringBootTest
@Transactional
class SymplaPipelineIntegrationTest {

    private static final String URL_FIXTURE =
            "https://www.sympla.com.br/evento/abba-world-tribute/3280023";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lamitrack")
            .withUsername("lamitrack")
            .withPassword("lamitrack");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    SymplaPipeline pipeline;

    @Autowired
    EventRepository eventRepository;

    @Test
    void persisteEventoExtraidoDaFixture() {
        boolean persistido = pipeline.processarHtml(htmlDaFixture());

        assertThat(persistido).isTrue();
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

    @Test
    void rodarPipelineDuasVezesNaoGeraLinhaDuplicada() {
        String html = htmlDaFixture();

        boolean primeira = pipeline.processarHtml(html);
        boolean segunda = pipeline.processarHtml(html);

        assertThat(primeira).isTrue();
        assertThat(segunda).isTrue();
        // Idempotência: mesmo evento raspado de novo não cria linha duplicada
        assertThat(eventRepository.count()).isEqualTo(1);

        Event unico = eventRepository.findByRegistrationUrl(URL_FIXTURE).orElseThrow();
        assertThat(unico.getTitle()).isEqualTo("ABBA World Tribute");
    }

    @Test
    void htmlSemEventoParseavelNaoPersistaNada() {
        boolean persistido = pipeline.processarHtml("<html><body><p>sem hidratação</p></body></html>");

        assertThat(persistido).isFalse();
        assertThat(eventRepository.count()).isZero();
    }

    private static String htmlDaFixture() {
        try {
            return Files.readString(
                    Path.of("src/test/resources/sympla-evento-abba-world-tribute.html"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
