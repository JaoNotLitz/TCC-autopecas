package br.com.wcautopecas.authorizer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.SecretKey;

/** Valida assinatura HS512 e expiração. Não consulta o Redis (sessão é checada nos serviços). */
public class ValidadorJwt {

    public record Identidade(String usuarioId, String papel) {
    }

    private final SecretKey chave;

    public ValidadorJwt(String chaveBase64) {
        this.chave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(chaveBase64.trim()));
    }

    /** Aceita o valor do header inteiro ("Bearer xxx"). Vazio se o token não vale. */
    public Optional<Identidade> validar(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        try {
            Claims c = Jwts.parser().verifyWith(chave).build()
                    .parseSignedClaims(authorization.substring(7).trim()).getPayload();
            String papel = c.get("papel", String.class);
            if (c.getSubject() == null || papel == null) {
                return Optional.empty();
            }
            return Optional.of(new Identidade(c.getSubject(), papel));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
