package br.com.lamit.lamitrack.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração fim a fim do endpoint {@code GET /eventos} com filtros e
 * paginação (#53): sobe a aplicação real (MockMvc) contra um Postgres real
 * via Testcontainers — mesmo padrão do {@code EventControllerIntegrationTest}
 * — e semeia os eventos via {@link EventRepository} antes de bater no
 * endpoint de verdade, sem mock de repository.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventListControllerIntegrationTest {

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
    void devolveTodosOsEventosPaginadosSemFiltro() throws Exception {
        salvarEventos(5);

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void filtraPorCity() throws Exception {
        salvarEvento("Festival A", "Aracaju", "SE", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 20, 9, 0));
        salvarEvento("Festival B", "Aracaju", "SE", Set.of("música"),
                LocalDateTime.of(2026, 9, 21, 10, 0));
        salvarEvento("Festival C", "Maceió", "AL", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 22, 11, 0));

        mockMvc.perform(get("/eventos").param("city", "Aracaju"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].city").value("Aracaju"))
                .andExpect(jsonPath("$.content[1].city").value("Aracaju"));
    }

    @Test
    void filtraPorUf() throws Exception {
        salvarEvento("Festival A", "Aracaju", "SE", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 20, 9, 0));
        salvarEvento("Festival B", "Maceió", "AL", Set.of("música"),
                LocalDateTime.of(2026, 9, 21, 10, 0));
        salvarEvento("Festival C", "Salvador", "BA", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 22, 11, 0));

        mockMvc.perform(get("/eventos").param("uf", "AL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].city").value("Maceió"))
                .andExpect(jsonPath("$.content[0].uf").value("AL"));
    }

    @Test
    void filtraPorTag() throws Exception {
        salvarEvento("Festival A", "Aracaju", "SE", Set.of("gastronomia", "música"),
                LocalDateTime.of(2026, 9, 20, 9, 0));
        salvarEvento("Festival B", "Maceió", "AL", Set.of("música"),
                LocalDateTime.of(2026, 9, 21, 10, 0));
        salvarEvento("Festival C", "Salvador", "BA", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 22, 11, 0));

        mockMvc.perform(get("/eventos").param("tag", "música"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tags", hasItem("música")))
                .andExpect(jsonPath("$.content[1].tags", hasItem("música")));
    }

    @Test
    void filtraPorDateCasoComODiaDeStartsAt() throws Exception {
        salvarEvento("Evento da Manhã", "Aracaju", "SE", Set.of(),
                LocalDateTime.of(2026, 9, 20, 9, 0));
        salvarEvento("Evento da Noite", "Aracaju", "SE", Set.of(),
                LocalDateTime.of(2026, 9, 20, 23, 30));
        salvarEvento("Evento do Dia Anterior", "Maceió", "AL", Set.of(),
                LocalDateTime.of(2026, 9, 19, 18, 0));
        salvarEvento("Evento do Dia Seguinte", "Salvador", "BA", Set.of(),
                LocalDateTime.of(2026, 9, 21, 10, 0));

        mockMvc.perform(get("/eventos").param("date", "2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].startsAt",
                        anyOf(equalTo("2026-09-20T09:00:00"),
                                equalTo("2026-09-20T23:30:00"))))
                .andExpect(jsonPath("$.content[1].startsAt",
                        anyOf(equalTo("2026-09-20T09:00:00"),
                                equalTo("2026-09-20T23:30:00"))));
    }

    @Test
    void combinaDoisFiltrosComAnd() throws Exception {
        salvarEvento("Festival A", "Aracaju", "SE", Set.of("música"),
                LocalDateTime.of(2026, 9, 20, 9, 0));
        salvarEvento("Festival B", "Aracaju", "SE", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 21, 10, 0));
        salvarEvento("Festival C", "Maceió", "AL", Set.of("música"),
                LocalDateTime.of(2026, 9, 22, 11, 0));

        mockMvc.perform(get("/eventos")
                        .param("city", "Aracaju")
                        .param("tag", "música"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Festival A"))
                .andExpect(jsonPath("$.content[0].city").value("Aracaju"))
                .andExpect(jsonPath("$.content[0].tags", hasItem("música")));
    }

    @Test
    void paginaComPageESize() throws Exception {
        salvarEventos(5);

        MvcResult pagina0 = mockMvc.perform(get("/eventos")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn();

        MvcResult pagina1 = mockMvc.perform(get("/eventos")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn();

        MvcResult pagina2 = mockMvc.perform(get("/eventos")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn();

        List<Long> idsPagina0 = idsDaPagina(pagina0);
        List<Long> idsPagina1 = idsDaPagina(pagina1);
        List<Long> idsPagina2 = idsDaPagina(pagina2);
        assertThat(idsPagina0).hasSize(2);
        assertThat(idsPagina1).hasSize(2);
        assertThat(idsPagina2).hasSize(1);
        assertThat(idsPagina0).doesNotContainAnyElementsOf(idsPagina1);
        assertThat(idsPagina0).doesNotContainAnyElementsOf(idsPagina2);
        assertThat(idsPagina1).doesNotContainAnyElementsOf(idsPagina2);
    }

    @Test
    void devolvePaginaVaziaQuandoNenhumEventoCorresponde() throws Exception {
        salvarEvento("Festival A", "Aracaju", "SE", Set.of("gastronomia"),
                LocalDateTime.of(2026, 9, 20, 9, 0));

        mockMvc.perform(get("/eventos").param("city", "Brasília"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private List<Long> idsDaPagina(MvcResult resultado) {
        return JsonPath.read(
                new String(resultado.getResponse().getContentAsByteArray(),
                        StandardCharsets.UTF_8),
                "$.content[*].id");
    }

    private void salvarEventos(int quantidade) {
        for (int i = 1; i <= quantidade; i++) {
            salvarEvento("Evento " + i, "Aracaju", "SE", Set.of("música"),
                    LocalDateTime.of(2026, 9, 20, 9, 0).plusHours(i));
        }
    }

    private void salvarEvento(String titulo,
                              String cidade,
                              String uf,
                              Set<String> tags,
                              LocalDateTime startsAt) {
        eventRepository.save(new Event(
                titulo,
                "Descrição de " + titulo,
                startsAt,
                "Local de " + titulo,
                cidade,
                uf,
                -10.9098,
                -37.0685,
                "https://www.sympla.com.br/evento/" + titulo.toLowerCase().replace(" ", "-"),
                "sympla",
                null,
                tags));
    }
}
