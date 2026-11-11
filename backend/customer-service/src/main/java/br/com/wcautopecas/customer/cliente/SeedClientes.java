package br.com.wcautopecas.customer.cliente;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seed do perfil local (HU-B00.4 RF03): 10 clientes PF/PJ com ids fixos (1 a 10), lidos de
 * {@code seed/clientes.json}. Só grava o que ainda não existe.
 */
@Component
@ConditionalOnProperty(prefix = "wc.clientes.seed", name = "habilitado", havingValue = "true")
public class SeedClientes implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedClientes.class);

    private final ClienteRepository repo;
    private final IdGenerator ids;
    private final ObjectMapper mapper;

    public SeedClientes(ClienteRepository repo, IdGenerator ids, ObjectMapper mapper) {
        this.repo = repo;
        this.ids = ids;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Map<String, Object>> clientes;
        try (InputStream in = getClass().getResourceAsStream("/seed/clientes.json")) {
            clientes = mapper.readValue(in, new TypeReference<>() {
            });
        }
        String agora = Instant.now().toString();
        int criados = 0;
        for (Map<String, Object> m : clientes) {
            long id = ((Number) m.get("id")).longValue();
            if (repo.porId(id).isPresent()) {
                continue;
            }
            ClienteItem c = new ClienteItem();
            c.setId(id);
            c.setTipo((String) m.get("tipo"));
            c.setDocumento((String) m.get("documento"));
            c.setNome((String) m.get("nome"));
            c.setNomeBusca(ClienteService.normalizar(c.getNome()));
            c.setEmail((String) m.get("email"));
            c.setTelefone((String) m.get("telefone"));
            c.setCidade((String) m.get("cidade"));
            c.setUf((String) m.get("uf"));
            c.setCep((String) m.get("cep"));
            c.setLogradouro((String) m.get("logradouro"));
            c.setNumero((String) m.get("numero"));
            c.setBairro((String) m.get("bairro"));
            c.setAtivo(true);
            c.setStatus("ativo");
            c.setTotalCompras(0L);
            c.setValorTotalCompras(0.0);
            c.setDataCriacao(agora);
            c.setDataAtualizacao(agora);
            repo.criar(c);
            criados++;
        }
        ids.garantirMinimo("clientes", clientes.size());
        log.info("seed: {} cliente(s) criado(s)", criados);
    }
}
