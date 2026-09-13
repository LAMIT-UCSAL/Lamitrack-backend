package br.com.lamit.lamitrack.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link JpaSpecificationExecutor} habilita a listagem dinâmica com filtros
 * combináveis da issue #53 (city, uf, tag, date).
 */
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Busca o evento pela URL de inscrição (issue #44): chave de idempotência
     * do pipeline, pois o {@code registrationUrl} contém o slug/id do evento
     * na Sympla e é único por evento (constraint {@code uq_events_registration_url}).
     */
    Optional<Event> findByRegistrationUrl(String registrationUrl);

    /**
     * Eventos ainda não enviados para o WhatsApp (issue #57):
     * {@code whatsappSentAt IS NULL} = "evento novo".
     */
    List<Event> findByWhatsappSentAtIsNull();
}
