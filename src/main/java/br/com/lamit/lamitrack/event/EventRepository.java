package br.com.lamit.lamitrack.event;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Busca o evento pela URL de inscrição (issue #44): chave de idempotência
     * do pipeline, pois o {@code registrationUrl} contém o slug/id do evento
     * na Sympla e é único por evento (constraint {@code uq_events_registration_url}).
     */
    Optional<Event> findByRegistrationUrl(String registrationUrl);
}
