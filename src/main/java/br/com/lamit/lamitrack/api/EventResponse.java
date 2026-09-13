package br.com.lamit.lamitrack.api;

import br.com.lamit.lamitrack.event.Event;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Contrato de resposta da API de eventos (issue #52): DTO próprio que espelha
 * todos os campos da entidade {@link Event} sem expor a entidade JPA
 * diretamente na resposta HTTP.
 */
public record EventResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startsAt,
        String venue,
        String city,
        String uf,
        Double latitude,
        Double longitude,
        String registrationUrl,
        String source,
        String bannerUrl,
        Set<String> tags) {

    public EventResponse {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartsAt(),
                event.getVenue(),
                event.getCity(),
                event.getUf(),
                event.getLatitude(),
                event.getLongitude(),
                event.getRegistrationUrl(),
                event.getSource(),
                event.getBannerUrl(),
                event.getTags());
    }
}
