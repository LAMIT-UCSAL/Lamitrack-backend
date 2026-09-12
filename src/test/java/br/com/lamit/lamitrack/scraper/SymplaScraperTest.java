package br.com.lamit.lamitrack.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.lamit.lamitrack.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do parse puro do Scraper da Sympla (#43). Rodam contra uma página
 * HTML REAL da Sympla salva como fixture (baixada de
 * https://www.sympla.com.br/evento/abba-world-tribute/3280023 em
 * 2026-02-10) — nenhuma chamada de rede durante o teste.
 */
class SymplaScraperTest {

    private SymplaScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new SymplaScraper(new ObjectMapper());
    }

    @Test
    void extraiEventoDaPaginaRealDaSympla() throws Exception {
        String html = htmlDaFixture();

        Optional<Event> evento = scraper.parse(html);

        assertThat(evento).isPresent();
        Event e = evento.get();
        assertThat(e.getTitle()).isEqualTo("ABBA World Tribute");
        assertThat(e.getDescription()).startsWith("ABBA WORLD TRIBUTEABBA WORLD TRIBUTE é um grupo brasileiro");
        assertThat(e.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 9, 26, 20, 0));
        assertThat(e.getVenue()).isEqualTo("Teatro Jardim Sul");
        assertThat(e.getCity()).isEqualTo("São Paulo");
        assertThat(e.getUf()).isEqualTo("SP");
        assertThat(e.getLatitude()).isEqualTo(-23.6312996);
        assertThat(e.getLongitude()).isEqualTo(-46.7360631);
        assertThat(e.getRegistrationUrl())
                .isEqualTo("https://www.sympla.com.br/evento/abba-world-tribute/3280023");
        assertThat(e.getSource()).isEqualTo("sympla");
        assertThat(e.getBannerUrl()).isEqualTo("https://images.sympla.com.br/696cdd550099c.jpg");
        assertThat(e.getTags()).containsExactly("musica");
    }

    @Test
    void paginaSemTagNextDataRetornaVazio() throws Exception {
        Optional<Event> evento = scraper.parse("<html><body><p>sem hidratação</p></body></html>");

        assertThat(evento).isEmpty();
    }

    @Test
    void nextDataSemCaminhoDeHidratacaoRetornaVazio() throws Exception {
        String html = """
                <html><body>
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"hydrationData":{"locationHydration":{"cities":[]}}}}}
                </script>
                </body></html>
                """;

        Optional<Event> evento = scraper.parse(html);

        assertThat(evento).isEmpty();
    }

    @Test
    void eventoSemDataDeInicioRetornaVazio() throws Exception {
        String html = htmlComEvento("{\"name\":\"Evento sem data\",\"startDate\":null}");

        Optional<Event> evento = scraper.parse(html);

        assertThat(evento).isEmpty();
    }

    @Test
    void eventoSemNomeRetornaVazio() throws Exception {
        String html = htmlComEvento("{\"name\":null,\"startDate\":\"2026-09-26 20:00:00\"}");

        Optional<Event> evento = scraper.parse(html);

        assertThat(evento).isEmpty();
    }

    @Test
    void camposOpcionaisAusentesVemNulosSemErro() throws Exception {
        String html = htmlComEvento(
                "{\"name\":\"Evento Mínimo\",\"startDate\":\"2026-09-26 20:00:00\","
                        + "\"newUrl\":\"https://www.sympla.com.br/evento/evento-minimo/1\"}");

        Optional<Event> evento = scraper.parse(html);

        assertThat(evento).isPresent();
        Event e = evento.get();
        assertThat(e.getTitle()).isEqualTo("Evento Mínimo");
        assertThat(e.getDescription()).isNull();
        assertThat(e.getVenue()).isNull();
        assertThat(e.getCity()).isNull();
        assertThat(e.getUf()).isNull();
        assertThat(e.getLatitude()).isNull();
        assertThat(e.getLongitude()).isNull();
        assertThat(e.getBannerUrl()).isNull();
        assertThat(e.getTags()).isEmpty();
    }

    @Test
    void jsonMalformadoLancaExcecao() {
        String html = """
                <html><body>
                <script id="__NEXT_DATA__" type="application/json">{json quebrado</script>
                </body></html>
                """;

        assertThatCode(() -> scraper.parse(html))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
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

    private static String htmlComEvento(String jsonEvento) {
        return """
                <html><body>
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"hydrationData":{"eventHydration":{"event":%s}}}}}
                </script>
                </body></html>
                """.formatted(jsonEvento);
    }
}
