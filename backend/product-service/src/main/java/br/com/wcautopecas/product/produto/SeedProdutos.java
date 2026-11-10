package br.com.wcautopecas.product.produto;

import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.product.api.ProdutoInput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seed do perfil local (HU-B00.4 RF02): 20 autopeças de {@code seed/produtos.json}, cobrindo os
 * quatro status de estoque. Só cria os códigos que ainda não existem.
 */
@Component
@ConditionalOnProperty(prefix = "wc.produtos.seed", name = "habilitado", havingValue = "true")
public class SeedProdutos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedProdutos.class);
    private static final UsuarioAutenticado SISTEMA = new UsuarioAutenticado(1, null, null, null, null);

    private final ProdutoService service;
    private final ProdutoRepository repo;
    private final ObjectMapper mapper;

    public SeedProdutos(ProdutoService service, ProdutoRepository repo, ObjectMapper mapper) {
        this.service = service;
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Map<String, Object>> produtos;
        try (InputStream in = getClass().getResourceAsStream("/seed/produtos.json")) {
            produtos = mapper.readValue(in, new TypeReference<>() {
            });
        }
        int criados = 0;
        for (Map<String, Object> m : produtos) {
            String codigo = (String) m.get("codigo");
            if (repo.porCodigo(codigo).isPresent()) {
                continue;
            }
            service.criar(new ProdutoInput(codigo, (String) m.get("nome"), (String) m.get("ncm"),
                    ((Number) m.get("estoqueAtual")).longValue(), ((Number) m.get("estoqueMinimo")).longValue(),
                    new BigDecimal(m.get("precoCusto").toString()), new BigDecimal(m.get("precoVenda").toString())),
                    SISTEMA);
            criados++;
        }
        log.info("seed: {} produto(s) criado(s)", criados);
    }
}
