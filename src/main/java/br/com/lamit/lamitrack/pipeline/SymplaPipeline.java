package br.com.lamit.lamitrack.pipeline;

import br.com.lamit.lamitrack.event.Event;
import br.com.lamit.lamitrack.event.EventRepository;
import br.com.lamit.lamitrack.scraper.SymplaScraper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline fim a fim da Sympla (issue #44): busca o HTML de uma URL de evento
 * via HTTP, extrai o evento com {@link SymplaScraper} (#43) e persiste no
 * banco via {@link EventRepository} (#42).
 *
 * <p>Idempotência: o {@code registrationUrl} contém o slug/id do evento na
 * Sympla e é único por evento (constraint {@code uq_events_registration_url},
 * migration V2). Rodar o pipeline duas vezes contra a mesma URL atualiza a
 * linha existente em vez de criar uma duplicada (upsert por
 * {@code registrationUrl}).
 *
 * <p>Disparo manual por enquanto (ver {@link PipelineConfiguration}); o
 * agendamento semanal é a issue #45.
 */
@Service
public class SymplaPipeline {

    private static final Logger log = LoggerFactory.getLogger(SymplaPipeline.class);

    private final HttpClient httpClient;
    private final SymplaScraper symplaScraper;
    private final EventRepository eventRepository;

    public SymplaPipeline(SymplaScraper symplaScraper, EventRepository eventRepository) {
        this.httpClient = HttpClient.newHttpClient();
        this.symplaScraper = symplaScraper;
        this.eventRepository = eventRepository;
    }

    /**
     * Roda o pipeline completo contra uma URL de evento da Sympla: busca o
     * HTML via HTTP, extrai o evento e persiste (upsert por
     * {@code registrationUrl}).
     *
     * @param url página de evento da Sympla
     * @return {@code true} se o evento foi persistido (novo ou atualizado);
     *         {@code false} se a busca falhou ou a página não contém evento
     *         parseável
     */
    public boolean processar(String url) {
        String html;
        try {
            html = buscarHtml(url);
        } catch (IOException e) {
            log.error("Falha ao buscar HTML de {}: {}", url, e.toString());
            return false;
        }
        return processarHtml(html);
    }

    /**
     * Roda o pipeline sem a etapa de rede: extrai o evento do HTML dado e
     * persiste (upsert por {@code registrationUrl}).
     *
     * @param html conteúdo HTML de uma página de evento da Sympla
     * @return {@code true} se o evento foi persistido (novo ou atualizado);
     *         {@code false} se o HTML não contém evento parseável
     */
    @Transactional
    public boolean processarHtml(String html) {
        try {
            Optional<Event> evento = symplaScraper.parse(html);
            if (evento.isEmpty()) {
                log.info("HTML não contém evento parseável; nada para persistir");
                return false;
            }
            persiste(evento.get());
            return true;
        } catch (JsonProcessingException e) {
            log.error("JSON malformado na página: {}", e.toString());
            return false;
        }
    }

    /**
     * Persiste o evento com upsert por {@code registrationUrl} (chave de
     * idempotência): se já existe um evento com a mesma URL de inscrição,
     * atualiza a linha em vez de inserir uma duplicada.
     */
    @Transactional
    public void persiste(Event evento) {
        if (evento.getRegistrationUrl() == null) {
            // sem chave de idempotência não dá para deduplicar: insere mesmo assim
            eventRepository.save(evento);
            return;
        }
        eventRepository.findByRegistrationUrl(evento.getRegistrationUrl())
                .ifPresentOrElse(
                        existente -> atualizar(existente, evento),
                        () -> eventRepository.save(evento));
    }

    private void atualizar(Event existente, Event raspado) {
        existente.setTitle(raspado.getTitle());
        existente.setDescription(raspado.getDescription());
        existente.setStartsAt(raspado.getStartsAt());
        existente.setVenue(raspado.getVenue());
        existente.setCity(raspado.getCity());
        existente.setUf(raspado.getUf());
        existente.setLatitude(raspado.getLatitude());
        existente.setLongitude(raspado.getLongitude());
        existente.setSource(raspado.getSource());
        existente.setBannerUrl(raspado.getBannerUrl());
        existente.setTags(raspado.getTags());
        eventRepository.save(existente);
    }

    private String buscarHtml(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Lamitrack/0.1 (+https://github.com/LAMIT-UCSAL/Lamitrack-backend)")
                .GET()
                .build();
        try {
            HttpResponse<String> resposta =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() / 100 != 2) {
                throw new IOException("HTTP " + resposta.statusCode() + " ao buscar " + url);
            }
            return resposta.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Busca de " + url + " interrompida", e);
        }
    }
}
