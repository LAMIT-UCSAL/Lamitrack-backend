package br.com.lamit.lamitrack.api;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventFilter;
import br.com.lamit.lamitrack.event.EventRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de eventos: busca um evento único pelo id (issue #52) e lista eventos
 * paginados com filtros opcionais combináveis por city, uf, tag e date
 * (issue #53), devolvendo sempre o DTO {@link EventResponse}.
 */
@RestController
@RequestMapping("/eventos")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Lista eventos paginados (issue #53). Os filtros são opcionais e se
     * combinam com AND; sem nenhum filtro devolve todos os eventos. A
     * paginação usa o {@link Pageable} padrão do Spring Data via query params
     * {@code page}/{@code size}/{@code sort}, e a resposta é a
     * {@link Page} serializada pelo Spring Data Web, sem envelope próprio.
     *
     * @param city cidade do evento (ex.: "Aracaju")
     * @param uf sigla da UF (ex.: "SE")
     * @param tag tag do evento (ex.: "gastronomia")
     * @param date dia ISO-8601 (ex.: 2026-09-20): casa com o dia de
     *             {@code startsAt} — eventos entre o início e o fim daquele dia
     */
    @GetMapping
    public Page<EventResponse> eventos(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable) {
        return eventRepository.findAll(EventFilter.of(city, uf, tag, date).toSpecification(), pageable)
                .map(EventResponse::from);
    }

    @GetMapping("/{id}")
    public EventResponse eventoPorId(@PathVariable Long id) {
        Optional<Event> evento = eventRepository.findById(id);
        return evento.map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento não encontrado: id=" + id));
    }
}
