package br.com.lamit.lamitrack.pipeline;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Prova que o {@code @Scheduled} do agendamento semanal (issue #45 / ADR 0003)
 * realmente dispara e chama o pipeline: usa um cron de teste bem curto (a cada
 * 2 segundos) e espera — sem depender de uma semana real — que o método
 * agendado chame {@link SymplaPipeline#processar(String)} pelo menos uma vez.
 *
 * <p>O {@link SymplaPipeline} é mockado para não fazer HTTP real; o foco é a
 * fiação do agendamento ({@code @EnableScheduling} + bean do scheduler + cron
 * curto). O disparo manual da #44 ({@code CommandLineRunner}) contribui no
 * máximo 1 chamada ao subir, então esperar 2+ chamadas prova que o
 * {@code @Scheduled} disparou pelo menos uma vez.
 */
@SpringBootTest(properties = {
        "lamitrack.pipeline.enabled=true",
        "lamitrack.pipeline.url=http://127.0.0.1:1/evento",
        "lamitrack.pipeline.cron=*/2 * * * * *"
})
class SymplaPipelineSchedulerFiringTest {

    private static final String URL = "http://127.0.0.1:1/evento";

    @MockBean
    SymplaPipeline pipeline;

    @Autowired
    SymplaPipelineScheduler scheduler;

    @Test
    void agendamentoDisparaEPassaACamarPipeline() {
        // scheduler precisa existir no contexto (prova que o bean foi criado)
        org.assertj.core.api.Assertions.assertThat(scheduler).isNotNull();

        verify(pipeline, timeout(Duration.ofSeconds(20).toMillis()).atLeast(2))
                .processar(URL);
    }
}
