package br.com.lamit.lamitrack.event;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Evento descoberto por um Scraper (ver CONTEXT.md): o contrato de dados
 * compartilhado entre scraper, API e site (issue #9).
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "venue")
    private String venue;

    @Column(name = "city")
    private String city;

    @Column(name = "uf")
    private String uf;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "registration_url")
    private String registrationUrl;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "banner_url")
    private String bannerUrl;

    /**
     * Quando o evento foi enviado para o grupo do WhatsApp (issue #57).
     * {@code null} = evento ainda não enviado ("evento novo").
     */
    @Column(name = "whatsapp_sent_at")
    private LocalDateTime whatsappSentAt;

    @ElementCollection
    @CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag")
    private Set<String> tags = new LinkedHashSet<>();

    protected Event() {
        // exigido pelo JPA
    }

    public Event(String title,
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
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.venue = venue;
        this.city = city;
        this.uf = uf;
        this.latitude = latitude;
        this.longitude = longitude;
        this.registrationUrl = registrationUrl;
        this.source = source;
        this.bannerUrl = bannerUrl;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getRegistrationUrl() {
        return registrationUrl;
    }

    public void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public LocalDateTime getWhatsappSentAt() {
        return whatsappSentAt;
    }

    public void setWhatsappSentAt(LocalDateTime whatsappSentAt) {
        this.whatsappSentAt = whatsappSentAt;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }
}
