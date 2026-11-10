package br.com.wcautopecas.commons.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.teste.ClientesTeste;
import br.com.wcautopecas.commons.teste.InfraTesteChaves;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class SegurancaRedisIT {

    final StringRedisTemplate redis = ClientesTeste.redis();
    final SessaoStore sessoes = new SessaoStore(redis);
    final JwtService jwt = JwtServiceTest.servico(Clock.systemUTC(), InfraTesteChaves.JWT_SECRET);
    final AutenticacaoFilter filtro = new AutenticacaoFilter(jwt, sessoes, List.of(), new ObjectMapper());

    @Test
    void tokenSemSessaoEh401EComSessaoPassa() {
        var t = jwt.emitir(99, Papel.VENDEDOR);
        assertThatThrownBy(() -> filtro.autenticar("Bearer " + t.token())).isInstanceOf(NaoAutenticadoException.class);
        sessoes.criar(99, t.token(), Duration.ofMinutes(5));
        assertThat(filtro.autenticar("Bearer " + t.token()).id()).isEqualTo(99);
    }

    @Test
    void logoutDerrubaSessao() {
        var t = jwt.emitir(98, Papel.ADMIN);
        sessoes.criar(98, t.token(), Duration.ofMinutes(5));
        sessoes.encerrar(98, t.token(), t.jti(), t.expiraEm());
        assertThat(sessoes.ativa(98, t.token(), t.jti())).isFalse();
        assertThat(redis.hasKey("blacklist:" + t.jti())).isTrue();
    }

    @Test
    void semBearerEh401() {
        assertThatThrownBy(() -> filtro.autenticar(null)).isInstanceOf(NaoAutenticadoException.class);
        assertThatThrownBy(() -> filtro.autenticar("Basic abc")).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void idempotencyGuardBloqueiaSegundaChamada() {
        IdempotencyGuard guard = new IdempotencyGuard(redis);
        String chave = "msg-" + UUID.randomUUID();
        assertThat(guard.primeiraVez(chave, Duration.ofMinutes(1))).isTrue();
        assertThat(guard.primeiraVez(chave, Duration.ofMinutes(1))).isFalse();
        guard.liberar(chave);
        assertThat(guard.primeiraVez(chave, Duration.ofMinutes(1))).isTrue();
    }
}
