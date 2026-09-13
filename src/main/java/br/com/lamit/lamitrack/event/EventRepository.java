package br.com.lamit.lamitrack.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    /**
     * Eventos ainda não enviados para o WhatsApp, limitados ao tamanho da
     * página (issue #58): {@code whatsappSentAt IS NULL} com {@code LIMIT},
     * em ordem de id para o lote ser determinístico (o resto fica para a
     * próxima execução).
     */
    List<Event> findByWhatsappSentAtIsNullOrderByIdAsc(Pageable pageable);

    /**
     * Quantidade de eventos enviados para o WhatsApp no intervalo
     * {@code [inicio, fim]} (issue #58): usada para calcular o que resta do
     * limite diário.
     */
    long countByWhatsappSentAtBetween(LocalDateTime inicio, LocalDateTime fim);
}
