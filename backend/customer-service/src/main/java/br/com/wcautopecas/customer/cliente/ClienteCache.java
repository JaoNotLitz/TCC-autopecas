package br.com.wcautopecas.customer.cliente;

import br.com.wcautopecas.customer.ClientesProperties;
import br.com.wcautopecas.customer.api.ClienteDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache Redis: {@code customer:{id}} (1 h) e buscas (5 min). As buscas usam uma "versão" na
 * chave; qualquer escrita incrementa a versão e invalida todas de uma vez, sem varrer chaves.
 * Falha do Redis nunca derruba a requisição: cai direto no DynamoDB.
 */
@Component
public class ClienteCache {

    private static final Logger log = LoggerFactory.getLogger(ClienteCache.class);
    private static final String VERSAO = "customers:versao";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ClientesProperties props;

    public ClienteCache(StringRedisTemplate redis, ObjectMapper mapper, ClientesProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
    }

    public List<ClienteDto> busca(String filtros, Supplier<List<ClienteDto>> carregar) {
        String chave = null;
        try {
            String versao = Optional.ofNullable(redis.opsForValue().get(VERSAO)).orElse("0");
            chave = "customers:busca:" + versao + ":" + filtros;
            String json = redis.opsForValue().get(chave);
            if (json != null) {
                return mapper.readValue(json, new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            log.warn("cache de busca indisponível: {}", e.getMessage());
        }
        List<ClienteDto> lista = carregar.get();
        gravar(chave, lista, props.getCacheBusca());
        return lista;
    }

    public Optional<ClienteDto> cliente(long id) {
        try {
            String json = redis.opsForValue().get("customer:" + id);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, ClienteDto.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void guardar(ClienteDto dto) {
        gravar("customer:" + dto.id(), dto, props.getCacheCliente());
    }

    /** Chamado em toda escrita de cliente. */
    public void invalidar(long id) {
        try {
            redis.delete("customer:" + id);
            redis.opsForValue().increment(VERSAO);
        } catch (Exception e) {
            log.warn("não consegui invalidar o cache do cliente {}: {}", id, e.getMessage());
        }
    }

    private void gravar(String chave, Object valor, java.time.Duration ttl) {
        if (chave == null) {
            return;
        }
        try {
            redis.opsForValue().set(chave, mapper.writeValueAsString(valor), ttl);
        } catch (Exception e) {
            log.warn("não consegui gravar no cache: {}", e.getMessage());
        }
    }
}
