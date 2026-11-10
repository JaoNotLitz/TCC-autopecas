package br.com.wcautopecas.commons.seguranca;

import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Emite e valida o JWT HS512 (claims: sub = id numérico, papel, jti, iat, exp). */
public class JwtService {

    public static final String MSG_TOKEN_INVALIDO = "Sessão expirada ou inválida. Faça login novamente.";

    private final ChaveJwt chave;
    private final Duration validade;
    private final Clock clock;

    public JwtService(ChaveJwt chave, Duration validade, Clock clock) {
        this.chave = chave;
        this.validade = validade;
        this.clock = clock;
    }

    public record TokenEmitido(String token, String jti, Instant expiraEm) {
    }

    public TokenEmitido emitir(long usuarioId, Papel papel) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(validade);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(Long.toString(usuarioId))
                .claim("papel", papel.name())
                .id(jti)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expira))
                .signWith(chave.get(), Jwts.SIG.HS512)
                .compact();
        return new TokenEmitido(token, jti, expira);
    }

    /** Valida assinatura e expiração; lança 401 se algo estiver errado. */
    public UsuarioAutenticado validar(String token) {
        if (token == null || token.isBlank()) {
            throw new NaoAutenticadoException(MSG_TOKEN_INVALIDO);
        }
        try {
            Claims c = Jwts.parser()
                    .verifyWith(chave.get())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new UsuarioAutenticado(Long.parseLong(c.getSubject()), Papel.valueOf(c.get("papel", String.class)),
                    token, c.getId(), c.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new NaoAutenticadoException(MSG_TOKEN_INVALIDO);
        }
    }
}
