package br.com.lamit.lamitrack.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

/**
 * Teste de unidade da configuração do agendamento semanal (issue #45 / ADR 0003):
 * a expressão cron configurada precisa ser válida/parseável pelo Spring, e o
 * default semanal precisa ser usado quando {@code lamitrack.pipeline.cron} não
 * é definido.
 */
class SymplaPipelineSchedulerTest {

    @Test
    void cronDefaultEhExpressaoValida() {
        assertThat(CronExpression.isValidExpression(PipelineProperties.CRON_DEFAULT)).isTrue();
        assertThat(CronExpression.parse(PipelineProperties.CRON_DEFAULT)).isNotNull();
    }

    @Test
    void cronEfetivoUsaDefaultQuandoCronNaoDefinido() {
        PipelineProperties props = new PipelineProperties(true, "https://exemplo.com/evento", null);

        assertThat(props.cronEfetivo()).isEqualTo(PipelineProperties.CRON_DEFAULT);
    }

    @Test
    void cronEfetivoUsaCronConfiguradoQuandoDefinido() {
        PipelineProperties props = new PipelineProperties(true, "https://exemplo.com/evento", "0 0 4 ? * MON");

        assertThat(props.cronEfetivo()).isEqualTo("0 0 4 ? * MON");
        assertThat(CronExpression.isValidExpression(props.cronEfetivo())).isTrue();
    }

    @Test
    void cronEfetivoTrataCronEmBrancoComoNaoDefinido() {
        PipelineProperties props = new PipelineProperties(true, "https://exemplo.com/evento", "   ");

        assertThat(props.cronEfetivo()).isEqualTo(PipelineProperties.CRON_DEFAULT);
    }

    @Test
    void cronInvalidoNaoEAceitoPeloSpring() {
        // Garante que o teste realmente valida: uma expressão malformada é rejeitada
        assertThatThrownBy(() -> CronExpression.parse("0 0 3 * *"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
