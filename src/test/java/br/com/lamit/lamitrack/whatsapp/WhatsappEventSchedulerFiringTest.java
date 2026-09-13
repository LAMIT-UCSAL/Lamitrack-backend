package br.com.lamit.lamitrack.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Prova que o {@code @Scheduled} do agendamento diário do WhatsApp (issue #58)
 * realmente dispara e chama o serviço: usa um cron de teste bem curto (a cada
 * 2 segundos) e espera — sem depender de um dia real — que o método agendado
 * chame {@link WhatsappEventService#processarEventosNovos(int)} pelo menos
 * uma vez (mesmo padrão do {@code SymplaPipelineSchedulerFiringTest}).
 *
 * <p>O {@link WhatsappEventService} é mockado para não fazer HTTP real; o foco
 * é a fiação do agendamento ({@code @EnableScheduling} + bean do scheduler +
 * cron curto). O disparo manual da #57 ({@code CommandLineRunner}) chama a
 * versão SEM argumento ({@code processarEventosNovos()}), então qualquer
 * chamada à versão COM argumento ({@code processarEventosNovos(int)}) só pode
 * vir do {@code @Scheduled} — provar que ela disparou é provar que o
 * agendamento funcionou.
 */
@SpringBootTest(properties = {
        "lamitrack.whatsapp.enabled=true",
        "lamitrack.whatsapp.base-url=http://127.0.0.1:1",
        "lamitrack.whatsapp.api-key=token",
        "lamitrack.whatsapp.group-id=120363@g.us",
        "lamitrack.whatsapp.cron=*/2 * * * * *"
})
class WhatsappEventSchedulerFiringTest {

    @MockBean
    WhatsappEventService whatsappEventService;

    @Autowired
    WhatsappEventScheduler scheduler;

    @Test
    void agendamentoDisparaEPassaAChamarServico() {
        // scheduler precisa existir no contexto (prova que o bean foi criado)
        assertThat(scheduler).isNotNull();

        // O agendamento só chama processarEventosNovos(int) se o restante do
        // limite diário for > 0; stub para garantir que ele avança.
        when(whatsappEventService.restanteDoLimiteDiario(anyInt())).thenReturn(5);

        // A versão COM argumento só é chamada pelo @Scheduled (o
        // CommandLineRunner da #57 chama a versão sem argumento).
        verify(whatsappEventService, timeout(Duration.ofSeconds(20).toMillis()).atLeast(1))
                .processarEventosNovos(anyInt());
    }
}
