package br.com.wcautopecas.entry.entrada;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.commons.validacao.Documentos;
import br.com.wcautopecas.entry.EntradasProperties;
import br.com.wcautopecas.entry.api.EntradaInput;
import br.com.wcautopecas.entry.api.NotaFiscalDto;
import br.com.wcautopecas.entry.integracao.Integracoes;
import br.com.wcautopecas.entry.integracao.Integracoes.CodigoProduto;
import br.com.wcautopecas.entry.xml.NfeEntradaParser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * EPB-11: nota de entrada do fornecedor. Registrar: resolve os produtos, grava o fornecedor,
 * pede a nota ENTRADA ao fiscal-service, grava {@code entry_notes} e soma o estoque. Prévia do XML:
 * só lê e devolve o formulário preenchido.
 */
@Service
public class EntradaService {

    private static final Logger log = LoggerFactory.getLogger(EntradaService.class);
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final EntradaRepository repo;
    private final Integracoes integracoes;
    private final IdGenerator ids;
    private final Outbox outbox;
    private final IdempotencyGuard locks;
    private final EntradasProperties props;
    private final Clock clock;

    public EntradaService(EntradaRepository repo, Integracoes integracoes, IdGenerator ids, Outbox outbox,
            IdempotencyGuard locks, EntradasProperties props, Clock clock) {
        this.repo = repo;
        this.integracoes = integracoes;
        this.ids = ids;
        this.outbox = outbox;
        this.locks = locks;
        this.props = props;
        this.clock = clock;
    }

    // ---- HU-B11.1

    public NotaFiscalDto registrar(EntradaInput in, UsuarioAutenticado usuario) {
        if (in.dataEntrada().isBefore(in.dataEmissao())) {
            throw new ValidacaoException("Data de entrada anterior à emissão",
                    List.of(new CampoErro("dataEntrada", "A data de entrada não pode ser anterior à emissão")));
        }
        if (in.dataEmissao().isAfter(hoje())) {
            throw new ValidacaoException("Data de emissão no futuro",
                    List.of(new CampoErro("dataEmissao", "A data de emissão não pode estar no futuro")));
        }
        String chave = Documentos.digitos(in.chaveAcesso());
        String cnpj = Documentos.digitos(in.fornecedor().cnpj());
        List<Long> produtos = resolverProdutos(in.itens());

        String lock = "entrada:" + chave;
        if (!locks.primeiraVez(lock, Duration.ofSeconds(30))) {
            throw new ConflitoException("Esta nota já está sendo registrada");
        }
        try {
            if (repo.porChave(chave).isPresent()) {
                throw new ConflitoException("Nota de entrada já registrada com esta chave de acesso");
            }
            Fornecedor f = fornecedor(in.fornecedor(), cnpj);
            long id = ids.proximo("entradas");

            List<ItemEntrada> itens = new ArrayList<>();
            List<Integracoes.ItemNota> itensNota = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < in.itens().size(); i++) {
                EntradaInput.Item it = in.itens().get(i);
                ItemEntrada e = new ItemEntrada();
                e.setProdutoId(produtos.get(i));
                e.setCodigo(it.codigo());
                e.setDescricao(it.descricao());
                e.setNcm(it.ncm());
                e.setCfop(it.cfop());
                e.setQuantidade(it.quantidade());
                e.setValorUnitario(it.valorUnitario());
                itens.add(e);
                itensNota.add(new Integracoes.ItemNota(produtos.get(i), it.codigo(), it.descricao(), it.ncm(),
                        it.cfop(), it.quantidade(), it.valorUnitario()));
                total = total.add(it.valorUnitario().multiply(BigDecimal.valueOf(it.quantidade())));
            }

            // RF05 / RNF: a nota fiscal vem antes de gravar e de mexer no estoque; se cair, nada fica gravado
            NotaFiscalDto nota = integracoes.registrarNota(new Integracoes.NotaEntradaFiscal(id, f.getId(),
                    in.numero(), in.serie(), chave, in.dataEmissao(), itensNota));

            NotaEntrada e = new NotaEntrada();
            e.setId(id);
            e.setStatus("registrada");
            e.setOrigem("manual");
            e.setFornecedorId(f.getId());
            e.setFornecedorCnpj(cnpj);
            e.setFornecedorNome(f.getRazaoSocial());
            e.setNumero(in.numero());
            e.setSerie(in.serie());
            e.setChaveAcesso(chave);
            e.setNaturezaOperacao(in.naturezaOperacao());
            e.setDataEmissao(in.dataEmissao().toString());
            e.setDataEntrada(in.dataEntrada().toString());
            e.setValorTotal(total.setScale(2, RoundingMode.HALF_EVEN));
            e.setNotaFiscalId(nota.id());
            e.setUsuarioId(usuario.id());
            e.setRegistradaEm(clock.instant().toString());
            e.setItens(itens);
            repo.criar(e);

            Map<Long, Long> somas = new LinkedHashMap<>();
            itens.forEach(x -> somas.merge(x.getProdutoId(), x.getQuantidade(), Long::sum));
            somas.forEach((produto, q) -> {
                Map<String, Object> op = new LinkedHashMap<>();
                op.put("type", "increase");
                op.put("produtoId", produto);
                op.put("quantidade", q);
                op.put("referenciaId", id);
                op.put("referenciaType", "entry");
                op.put("usuarioId", usuario.id());
                op.put("motivo", "Entrada NF " + in.numero() + " de " + f.getRazaoSocial());
                outbox.fila(props.getFilaEstoque(), "increase", op);
            });
            log.info("entrada {} registrada: fornecedor {}, nota {}, {} itens", id, cnpj, nota.id(), itens.size());
            return nota;
        } finally {
            locks.liberar(lock);
        }
    }

    /** Produto pelo id informado ou pelo código no catálogo; códigos desconhecidos → 422 (decisão A5). */
    private List<Long> resolverProdutos(List<EntradaInput.Item> itens) {
        List<String> codigos = itens.stream().filter(i -> i.produtoId() == null)
                .map(i -> i.codigo() == null ? "" : i.codigo().trim()).distinct().toList();
        Map<String, CodigoProduto> achados = integracoes.produtosPorCodigo(codigos).stream()
                .collect(Collectors.toMap(c -> c.codigo().trim(), Function.identity(), (a, b) -> a));
        List<Long> ids = new ArrayList<>();
        List<CampoErro> faltam = new ArrayList<>();
        List<String> desconhecidos = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            EntradaInput.Item it = itens.get(i);
            if (it.produtoId() != null) {
                ids.add(it.produtoId());
                continue;
            }
            String codigo = it.codigo() == null ? "" : it.codigo().trim();
            CodigoProduto p = achados.get(codigo);
            if (p == null || p.produtoId() == null) {
                faltam.add(new CampoErro("itens[" + i + "].codigo", "Produto não cadastrado"));
                desconhecidos.add(codigo.isEmpty() ? "(sem código)" : codigo);
                ids.add(null);
            } else {
                ids.add(p.produtoId());
            }
        }
        if (!faltam.isEmpty()) {
            throw new RegraNegocioException("Produtos não cadastrados: " + String.join(", ", desconhecidos), faltam);
        }
        return ids;
    }

    /** Cria o fornecedor (id numérico) ou atualiza os dados dele; a chave é o CNPJ. */
    private Fornecedor fornecedor(EntradaInput.Fornecedor in, String cnpj) {
        String agora = clock.instant().toString();
        Fornecedor f = repo.fornecedor(cnpj).orElse(null);
        if (f == null) {
            f = new Fornecedor();
            f.setCnpj(cnpj);
            f.setId(ids.proximo("fornecedores"));
            f.setRazaoSocial(in.razaoSocial().trim());
            f.setIe(in.ie());
            f.setUf(in.uf());
            f.setCriadoEm(agora);
            f.setAtualizadoEm(agora);
            if (repo.criarFornecedor(f)) {
                return f;
            }
            f = repo.fornecedor(cnpj).orElseThrow();
        }
        f.setRazaoSocial(in.razaoSocial().trim());
        if (in.ie() != null && !in.ie().isBlank()) {
            f.setIe(in.ie());
        }
        if (in.uf() != null && !in.uf().isBlank()) {
            f.setUf(in.uf());
        }
        f.setAtualizadoEm(agora);
        repo.salvarFornecedor(f);
        return f;
    }

    // ---- HU-B11.2

    public EntradaInput previa(byte[] xml) {
        EntradaInput lido = NfeEntradaParser.ler(xml, hoje());
        if (lido.chaveAcesso() != null && repo.porChave(lido.chaveAcesso()).isPresent()) {
            throw new ConflitoException("Nota de entrada já registrada com esta chave de acesso");
        }
        List<String> codigos = lido.itens().stream().map(EntradaInput.Item::codigo)
                .filter(c -> c != null && !c.isBlank()).distinct().toList();
        Map<String, Long> ids = new LinkedHashMap<>();
        integracoes.produtosPorCodigo(codigos).forEach(c -> {
            if (c.produtoId() != null) {
                ids.put(c.codigo(), c.produtoId());
            }
        });
        List<EntradaInput.Item> itens = lido.itens().stream().map(i -> new EntradaInput.Item(ids.get(i.codigo()),
                i.codigo(), i.descricao(), i.ncm(), i.cfop(), i.quantidade(), i.valorUnitario())).toList();
        return new EntradaInput(lido.fornecedor(), lido.numero(), lido.serie(), lido.dataEmissao(),
                lido.dataEntrada(), lido.chaveAcesso(), lido.naturezaOperacao(), itens);
    }

    // ---- HU-B11.4 (extra)

    public List<NotaEntrada> listar(Long fornecedorId) {
        List<NotaEntrada> todas = fornecedorId == null ? repo.todas() : repo.doFornecedor(fornecedorId);
        return todas.stream().sorted(Comparator.comparing(NotaEntrada::getId).reversed()).toList();
    }

    public NotaEntrada buscar(long id) {
        return repo.porId(id).orElseThrow(() -> new NaoEncontradoException("Entrada não encontrada"));
    }

    private LocalDate hoje() {
        return LocalDate.now(clock.withZone(FUSO));
    }
}
