package br.com.lamit.lamitrack.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combináveis (AND) da listagem {@code GET /eventos} (issue #53):
 * city, uf, tag e date. Cada filtro é opcional — quando um parâmetro é
 * {@code null} a cláusula correspondente não entra na query, e sem nenhum
 * filtro a Specification devolve todos os eventos.
 *
 * <p>O filtro por tag navega a coleção {@code Event.tags}
 * ({@code @ElementCollection}, tabela {@code event_tags}) via o operador
 * "member of" do JPA ({@code CriteriaBuilder#isMember}), que o Hibernate
 * traduz em um {@code EXISTS} sobre a tabela de coleção.
 */
public final class EventFilter {

    private final String city;
    private final String uf;
    private final String tag;
    private final LocalDate date;

    private EventFilter(String city, String uf, String tag, LocalDate date) {
        this.city = city;
        this.uf = uf;
        this.tag = tag;
        this.date = date;
    }

    public static EventFilter of(String city, String uf, String tag, LocalDate date) {
        return new EventFilter(city, uf, tag, date);
    }

    public Specification<Event> toSpecification() {
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();
        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("city"), city));
        }
        if (uf != null && !uf.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("uf"), uf));
        }
        if (tag != null && !tag.isBlank()) {
            // Operador "member of" do JPA 3.1+ (cb.isMember): o Hibernate
            // traduz em EXISTS sobre a tabela event_tags.
            spec = spec.and((root, query, cb) -> cb.isMember(tag, root.get("tags")));
        }
        if (date != null) {
            LocalDateTime inicioDoDia = date.atStartOfDay();
            LocalDateTime fimDoDia = date.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.between(
                    root.get("startsAt"), inicioDoDia, fimDoDia.minusNanos(1)));
        }
        return spec;
    }
}
