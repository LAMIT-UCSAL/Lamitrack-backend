package br.com.lamit.lamitrack.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
 * Integração do EventRepository contra um Postgres real via Testcontainers (#42).
 * O container é isolado: o datasource do application.yml (H2 em memória) não é
 * usado aqui — as propriedades do datasource são apontadas para o container.
 */
@Testcontainers
@SpringBootTest
@Transactional
class EventRepositoryIntegrationTest {

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
    EventRepository eventRepository;

    @Test
    void salvaERecuperaEventoComTodosOsCampos() {
        Event evento = new Event(
                "Churrasco Carvão 2026",
                "Festival de churrasco com shows ao vivo",
                LocalDateTime.of(2026, 11, 20, 18, 0),
                "Parque de Exposições",
                "Aracaju",
                "SE",
                -10.9098,
                -37.0685,
                "https://www.sympla.com.br/evento/churrascarva-2026/3521412",
                "sympla",
                "https://www.sympla.com.br/banner/churrasco.jpg",
                Set.of("gastronomia", "música"));

        Event salvo = eventRepository.save(evento);
        assertThat(salvo.getId()).isNotNull();

        List<Event> encontrados = eventRepository.findAll();
        assertThat(encontrados).hasSize(1);

        Event recuperado = encontrados.get(0);
        assertThat(recuperado.getId()).isEqualTo(salvo.getId());
        assertThat(recuperado.getTitle()).isEqualTo("Churrasco Carvão 2026");
        assertThat(recuperado.getDescription()).isEqualTo("Festival de churrasco com shows ao vivo");
        assertThat(recuperado.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 18, 0));
        assertThat(recuperado.getVenue()).isEqualTo("Parque de Exposições");
        assertThat(recuperado.getCity()).isEqualTo("Aracaju");
        assertThat(recuperado.getUf()).isEqualTo("SE");
        assertThat(recuperado.getLatitude()).isEqualTo(-10.9098);
        assertThat(recuperado.getLongitude()).isEqualTo(-37.0685);
        assertThat(recuperado.getRegistrationUrl())
                .isEqualTo("https://www.sympla.com.br/evento/churrascarva-2026/3521412");
        assertThat(recuperado.getSource()).isEqualTo("sympla");
        assertThat(recuperado.getBannerUrl()).isEqualTo("https://www.sympla.com.br/banner/churrasco.jpg");
        assertThat(recuperado.getTags()).containsExactlyInAnyOrder("gastronomia", "música");
    }
}
