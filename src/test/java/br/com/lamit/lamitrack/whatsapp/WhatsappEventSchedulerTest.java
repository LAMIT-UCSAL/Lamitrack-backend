package br.com.lamit.lamitrack.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

/**
 * Teste de unidade da configuração do agendamento diário do WhatsApp
 * (issue #58): a expressão cron configurada precisa ser válida/parseável pelo
 * Spring, o default diário precisa ser usado quando
 * {@code lamitrack.whatsapp.cron} não é definido, e o limite diário precisa
 * ter default razoável quando {@code lamitrack.whatsapp.daily-limit} não é
 * definido (mesmo padrão de {@code SymplaPipelineSchedulerTest}).
 */
class WhatsappEventSchedulerTest {

    @Test
    void cronDefaultEhExpressaoValida() {
        assertThat(CronExpression.isValidExpression(WhatsappProperties.CRON_DEFAULT)).isTrue();
        assertThat(CronExpression.parse(WhatsappProperties.CRON_DEFAULT)).isNotNull();
    }

    @Test
    void cronEfetivoUsaDefaultQuandoCronNaoDefinido() {
        WhatsappProperties props = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, null, null);

        assertThat(props.cronEfetivo()).isEqualTo(WhatsappProperties.CRON_DEFAULT);
    }

    @Test
    void cronEfetivoUsaCronConfiguradoQuandoDefinido() {
        WhatsappProperties props = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, "0 30 8 * * ?", null);

        assertThat(props.cronEfetivo()).isEqualTo("0 30 8 * * ?");
        assertThat(CronExpression.isValidExpression(props.cronEfetivo())).isTrue();
    }

    @Test
    void cronEfetivoTrataCronEmBrancoComoNaoDefinido() {
        WhatsappProperties props = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, "   ", null);

        assertThat(props.cronEfetivo()).isEqualTo(WhatsappProperties.CRON_DEFAULT);
    }

    @Test
    void cronInvalidoNaoEAceitoPeloSpring() {
        // Garante que o teste realmente valida: uma expressão malformada é rejeitada
        assertThatThrownBy(() -> CronExpression.parse("0 0 9 * *"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyLimitEfetivoUsaDefaultQuandoNaoDefinido() {
        WhatsappProperties props = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, null, null);

        assertThat(props.dailyLimitEfetivo()).isEqualTo(WhatsappProperties.DAILY_LIMIT_DEFAULT);
    }

    @Test
    void dailyLimitEfetivoUsaValorConfiguradoQuandoDefinido() {
        WhatsappProperties props = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, null, 5);

        assertThat(props.dailyLimitEfetivo()).isEqualTo(5);
    }

    @Test
    void dailyLimitEfetivoTrataValorInvalidoComoNaoDefinido() {
        WhatsappProperties propsZero = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, null, 0);
        WhatsappProperties propsNegativo = new WhatsappProperties(true, "http://evolution:8080",
                "token", "120363@g.us", null, null, -3);

        assertThat(propsZero.dailyLimitEfetivo()).isEqualTo(WhatsappProperties.DAILY_LIMIT_DEFAULT);
        assertThat(propsNegativo.dailyLimitEfetivo()).isEqualTo(WhatsappProperties.DAILY_LIMIT_DEFAULT);
    }
}
