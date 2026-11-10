package br.com.wcautopecas.commons.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Token do usuário técnico "sistema" (id 0, ADMIN) para chamadas entre serviços feitas fora de
 * uma requisição, como num listener de fila. É um JWT comum com sessão no Redis, emitido pelo
 * próprio serviço com o segredo compartilhado, e renovado um minuto antes de expirar.
 */
public class TokenServico {

    public static final long USUARIO_SISTEMA = 0;
    private static final Duration FOLGA = Duration.ofMinutes(1);

    private final JwtService jwt;
    private final SessaoStore sessoes;
    private final Clock clock;
    private JwtService.TokenEmitido atual;

    public TokenServico(JwtService jwt, SessaoStore sessoes, Clock clock) {
        this.jwt = jwt;
        this.sessoes = sessoes;
        this.clock = clock;
    }

    public synchronized String token() {
        Instant agora = clock.instant();
        if (atual == null || atual.expiraEm().minus(FOLGA).isBefore(agora)) {
            atual = jwt.emitir(USUARIO_SISTEMA, Papel.ADMIN);
            sessoes.criar(USUARIO_SISTEMA, atual.token(), Duration.between(agora, atual.expiraEm()));
        }
        return atual.token();
    }
}
