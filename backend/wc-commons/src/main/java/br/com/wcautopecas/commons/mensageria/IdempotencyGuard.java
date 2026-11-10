package br.com.wcautopecas.commons.mensageria;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Evita processar a mesma mensagem duas vezes: {@code SET lock:{chave} NX} com TTL. */
public class IdempotencyGuard {

    private final StringRedisTemplate redis;

    public IdempotencyGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** {@code true} se esta é a primeira vez que a chave aparece dentro do TTL. */
    public boolean primeiraVez(String chave, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("lock:" + chave, "1", ttl));
    }

    /** Libera a chave (quando o processamento falhou e a mensagem deve ser reprocessada). */
    public void liberar(String chave) {
        redis.delete("lock:" + chave);
    }
}
