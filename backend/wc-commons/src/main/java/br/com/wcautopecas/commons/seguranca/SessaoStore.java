package br.com.wcautopecas.commons.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Sessões no Redis: {@code session:{userId}:{sha256(token)}} com TTL igual à validade do token,
 * e blacklist de {@code jti} após logout.
 */
public class SessaoStore {

    private final StringRedisTemplate redis;

    public SessaoStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void criar(long usuarioId, String token, Duration ttl) {
        redis.opsForValue().set(chave(usuarioId, token), Instant.now().toString(), ttl);
    }

    public boolean ativa(long usuarioId, String token, String jti) {
        if (jti != null && Boolean.TRUE.equals(redis.hasKey("blacklist:" + jti))) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(chave(usuarioId, token)));
    }

    public void encerrar(long usuarioId, String token, String jti, Instant expiraEm) {
        redis.delete(chave(usuarioId, token));
        Duration restante = Duration.between(Instant.now(), expiraEm);
        if (jti != null && !restante.isNegative() && !restante.isZero()) {
            redis.opsForValue().set("blacklist:" + jti, "1", restante);
        }
    }

    static String chave(long usuarioId, String token) {
        return "session:" + usuarioId + ":" + sha256(token);
    }

    static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
