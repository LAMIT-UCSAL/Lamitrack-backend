package br.com.lamit.lamitrack.api;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de eventos (issue #52): busca um evento único pelo id, devolvendo o
 * DTO {@link EventResponse}. Listagem, paginação e filtros ficam para a
 * issue #53.
 */
@RestController
@RequestMapping("/eventos")
public class EventController {

    private final EventRepository eventRepository;

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/{id}")
    public EventResponse eventoPorId(@PathVariable Long id) {
        Optional<Event> evento = eventRepository.findById(id);
        return evento.map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento não encontrado: id=" + id));
    }
}
