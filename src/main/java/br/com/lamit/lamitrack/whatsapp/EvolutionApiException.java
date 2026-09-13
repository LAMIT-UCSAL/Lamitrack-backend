package br.com.lamit.lamitrack.whatsapp;

/**
 * Falha no envio de mensagem via Evolution API (issue #57): status HTTP
 * não-2xx, falha de rede ou corpo malformado. Lançada pelo
 * {@link EvolutionApiClient} e tratada evento a evento pelo
 * {@link WhatsappEventService} (uma falha não derruba o lote).
 */
public class EvolutionApiException extends RuntimeException {

    public EvolutionApiException(String message) {
        super(message);
    }

    public EvolutionApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
