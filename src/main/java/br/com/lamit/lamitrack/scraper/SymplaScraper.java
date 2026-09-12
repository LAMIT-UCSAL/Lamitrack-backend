package br.com.lamit.lamitrack.scraper;

import br.com.lamit.lamitrack.event.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Scraper da Sympla (ver CONTEXT.md): recebe o HTML de uma página de evento
 * da Sympla e extrai o evento embutido no JSON de hidratação do Next.js,
 * dentro da tag {@code <script id="__NEXT_DATA__">}, mapeando para a entidade
 * {@link Event} (#42).
 *
 * <p>Parse puro: não faz chamada de rede nem persistência (o pipeline é a
 * issue #44). O caminho de referência dentro do JSON é
 * {@code props.pageProps.hydrationData.eventHydration.event} (prova manual
 * da POC-002).
 *
 * <p>Mapeamento dos campos do JSON para a entidade:
 * <ul>
 *   <li>{@code name} → {@code title}</li>
 *   <li>{@code startDate} → {@code startsAt} (formato {@code yyyy-MM-dd HH:mm:ss})</li>
 *   <li>{@code strippedDetail} → {@code description}</li>
 *   <li>{@code eventsCategory.name} → {@code tags} (uma tag por categoria)</li>
 *   <li>{@code newUrl} → {@code registrationUrl} (já contém o {@code id} do evento)</li>
 *   <li>{@code eventsAddress.name/city/state} → {@code venue/city/uf}</li>
 *   <li>{@code eventsAddress.geolocation.latitude/longitude} → {@code latitude/longitude}</li>
 *   <li>{@code logoUrl} → {@code bannerUrl} (a Sympla não expõe campo "bannerUrl";
 *       o banner da página é o logo do evento)</li>
 *   <li>{@code source} → {@code "sympla"}</li>
 * </ul>
 *
 * <p>Fora do mapeamento por falta de campo correspondente em {@link Event}:
 * {@code eventsHost.name} (organizador) e {@code endDate} (a entidade não tem
 * data de fim).
 */
public class SymplaScraper {

    private static final String SOURCE = "sympla";
    private static final DateTimeFormatter SYMPLA_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    public SymplaScraper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extrai o evento de uma página de evento da Sympla.
     *
     * @param html conteúdo HTML da página de evento
     * @return o evento mapeado, ou vazio se a página não tem a tag
     *         {@code __NEXT_DATA__}, se o caminho de hidratação não existe ou
     *         se os campos obrigatórios ({@code name}, {@code startDate})
     *         estão ausentes/inválidos
     * @throws JsonProcessingException se o JSON embutido na página estiver malformado
     */
    public Optional<Event> parse(String html) throws JsonProcessingException {
        Document document = Jsoup.parse(html);
        Element nextData = document.selectFirst("script#__NEXT_DATA__");
        if (nextData == null) {
            return Optional.empty();
        }

        JsonNode event = objectMapper.readTree(nextData.data())
                .path("props")
                .path("pageProps")
                .path("hydrationData")
                .path("eventHydration")
                .path("event");
        if (!event.isObject()) {
            return Optional.empty();
        }

        String title = text(event, "name");
        LocalDateTime startsAt = parseStartsAt(event.path("startDate"));
        if (title == null || startsAt == null) {
            return Optional.empty();
        }

        JsonNode address = event.path("eventsAddress");
        JsonNode geolocation = address.path("geolocation");

        Set<String> tags = new LinkedHashSet<>();
        String categoria = text(event.path("eventsCategory"), "name");
        if (categoria != null) {
            tags.add(categoria);
        }

        return Optional.of(new Event(
                title,
                text(event, "strippedDetail"),
                startsAt,
                text(address, "name"),
                text(address, "city"),
                text(address, "state"),
                geolocation.path("latitude").isNumber() ? geolocation.path("latitude").asDouble() : null,
                geolocation.path("longitude").isNumber() ? geolocation.path("longitude").asDouble() : null,
                text(event, "newUrl"),
                SOURCE,
                text(event, "logoUrl"),
                tags));
    }

    /**
     * @return o texto do campo se ele existir, for textual e não for em branco; senão {@code null}
     */
    private static String text(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isTextual() && !valor.asText().isBlank() ? valor.asText() : null;
    }

    private static LocalDateTime parseStartsAt(JsonNode startDate) {
        if (!startDate.isTextual()) {
            return null;
        }
        try {
            return LocalDateTime.parse(startDate.asText(), SYMPLA_DATA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
