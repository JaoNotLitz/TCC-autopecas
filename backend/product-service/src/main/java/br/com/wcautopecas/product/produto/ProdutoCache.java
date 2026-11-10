package br.com.wcautopecas.product.produto;

import br.com.wcautopecas.product.ProdutosProperties;
import br.com.wcautopecas.product.api.ProdutoDto;
import br.com.wcautopecas.product.api.ResumoEstoque;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Cache Redis {@code product:{id}} (1 h) e {@code product:resumo} (5 min). Falha do Redis não derruba nada. */
@Component
public class ProdutoCache {

    private static final Logger log = LoggerFactory.getLogger(ProdutoCache.class);
    private static final String RESUMO = "product:resumo";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ProdutosProperties props;

    public ProdutoCache(StringRedisTemplate redis, ObjectMapper mapper, ProdutosProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
    }

    public Optional<ProdutoDto> produto(long id) {
        return ler("product:" + id, ProdutoDto.class);
    }

    public void guardar(ProdutoDto dto) {
        gravar("product:" + dto.id(), dto, props.getCacheProduto());
    }

    public ResumoEstoque resumo(Supplier<ResumoEstoque> calcular) {
        return ler(RESUMO, ResumoEstoque.class).orElseGet(() -> {
            ResumoEstoque r = calcular.get();
            gravar(RESUMO, r, props.getCacheResumo());
            return r;
        });
    }

    public void invalidar(long produtoId) {
        try {
            redis.delete(java.util.List.of("product:" + produtoId, RESUMO));
        } catch (Exception e) {
            log.warn("não consegui invalidar o cache do produto {}: {}", produtoId, e.getMessage());
        }
    }

    private <T> Optional<T> ler(String chave, Class<T> tipo) {
        try {
            String json = redis.opsForValue().get(chave);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, tipo));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void gravar(String chave, Object valor, Duration ttl) {
        try {
            redis.opsForValue().set(chave, mapper.writeValueAsString(valor), ttl);
        } catch (Exception e) {
            log.warn("não consegui gravar {} no cache: {}", chave, e.getMessage());
        }
    }
}
