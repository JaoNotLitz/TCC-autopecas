package br.com.wcautopecas.commons.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.config.WcProperties;
import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import br.com.wcautopecas.commons.teste.InfraTesteChaves;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    static JwtService servico(Clock clock, String secret) {
        WcProperties.Jwt cfg = new WcProperties.Jwt();
        cfg.setSecret(secret);
        return new JwtService(new ChaveJwt(cfg, null), Duration.ofHours(8), clock);
    }

    @Test
    void emiteEValida() {
        JwtService jwt = servico(Clock.systemUTC(), InfraTesteChaves.JWT_SECRET);
        var emitido = jwt.emitir(7, Papel.ADMIN);
        UsuarioAutenticado u = jwt.validar(emitido.token());
        assertThat(u.id()).isEqualTo(7);
        assertThat(u.papel()).isEqualTo(Papel.ADMIN);
        assertThat(u.jti()).isEqualTo(emitido.jti());
        assertThat(emitido.token().split("\\.")[0]).isEqualTo("eyJhbGciOiJIUzUxMiJ9");
    }

    @Test
    void tokenExpiradoRecusado() {
        Clock passado = Clock.fixed(Instant.now().minus(Duration.ofHours(9)), ZoneOffset.UTC);
        String token = servico(passado, InfraTesteChaves.JWT_SECRET).emitir(1, Papel.VENDEDOR).token();
        assertThatThrownBy(() -> servico(Clock.systemUTC(), InfraTesteChaves.JWT_SECRET).validar(token))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void assinaturaDeOutraChaveRecusada() {
        String outra = java.util.Base64.getEncoder().encodeToString(new byte[64]);
        String token = servico(Clock.systemUTC(), outra).emitir(1, Papel.ADMIN).token();
        assertThatThrownBy(() -> servico(Clock.systemUTC(), InfraTesteChaves.JWT_SECRET).validar(token))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void lixoRecusado() {
        JwtService jwt = servico(Clock.systemUTC(), InfraTesteChaves.JWT_SECRET);
        assertThatThrownBy(() -> jwt.validar("abc.def.ghi")).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> jwt.validar("")).isInstanceOf(NaoAutenticadoException.class);
    }
}
