package br.com.lamit.lamitrack.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração fim a fim do endpoint {@code GET /eventos/{id}} (#52): sobe a
 * aplicação real (MockMvc) contra um Postgres real via Testcontainers — mesmo
 * padrão do {@code EventRepositoryIntegrationTest} — e semeia o evento via
 * {@link EventRepository} antes de bater no endpoint de verdade, sem mock de
 * repository.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIntegrationTest {

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
    MockMvc mockMvc;

    @Autowired
    EventRepository eventRepository;

    @Test
    void devolveEventoCompletoQuandoIdExiste() throws Exception {
        Event salvo = eventRepository.save(new Event(
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
                Set.of("gastronomia", "música")));

        mockMvc.perform(get("/eventos/{id}", salvo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.title").value("Churrasco Carvão 2026"))
                .andExpect(jsonPath("$.description").value("Festival de churrasco com shows ao vivo"))
                .andExpect(jsonPath("$.startsAt").value("2026-11-20T18:00:00"))
                .andExpect(jsonPath("$.venue").value("Parque de Exposições"))
                .andExpect(jsonPath("$.city").value("Aracaju"))
                .andExpect(jsonPath("$.uf").value("SE"))
                .andExpect(jsonPath("$.latitude").value(-10.9098))
                .andExpect(jsonPath("$.longitude").value(-37.0685))
                .andExpect(jsonPath("$.registrationUrl")
                        .value("https://www.sympla.com.br/evento/churrascarva-2026/3521412"))
                .andExpect(jsonPath("$.source").value("sympla"))
                .andExpect(jsonPath("$.bannerUrl")
                        .value("https://www.sympla.com.br/banner/churrasco.jpg"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("gastronomia", "música")));
    }

    @Test
    void devolve404QuandoIdNaoExiste() throws Exception {
        mockMvc.perform(get("/eventos/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }
}
