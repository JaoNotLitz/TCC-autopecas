package br.com.wcautopecas.product.produto;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.UsuarioAtual;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.product.ProdutosProperties;
import br.com.wcautopecas.product.api.Internos.AjusteAceito;
import br.com.wcautopecas.product.api.Internos.AjusteInput;
import br.com.wcautopecas.product.api.Internos.Disponibilidade;
import br.com.wcautopecas.product.api.Internos.ItemDisponibilidade;
import br.com.wcautopecas.product.api.Internos.Movimento;
import br.com.wcautopecas.product.api.Internos.ProdutoFiscal;
import br.com.wcautopecas.product.api.ProdutoDto;
import br.com.wcautopecas.product.api.ProdutoInput;
import br.com.wcautopecas.product.api.ResumoEstoque;
import br.com.wcautopecas.product.estoque.EstoqueException;
import br.com.wcautopecas.product.estoque.EstoqueService;
import br.com.wcautopecas.product.estoque.MovimentoItem;
import br.com.wcautopecas.product.estoque.Operacao;
import br.com.wcautopecas.product.estoque.StatusEstoque;
import br.com.wcautopecas.product.produto.ProdutoRepository.VersaoDesatualizadaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    static final String MOTIVO_TELA = "Ajuste pela tela de estoque";
    private static final Collator ALFABETICA = Collator.getInstance(Locale.forLanguageTag("pt-BR"));

    static {
        ALFABETICA.setStrength(Collator.PRIMARY);
    }

    private final ProdutoRepository repo;
    private final EstoqueService estoque;
    private final ProdutoCache cache;
    private final IdGenerator ids;
    private final EventPublisher publisher;
    private final ProdutosProperties props;
    private final Clock clock;

    public ProdutoService(ProdutoRepository repo, EstoqueService estoque, ProdutoCache cache, IdGenerator ids,
            EventPublisher publisher, ProdutosProperties props, Clock clock) {
        this.repo = repo;
        this.estoque = estoque;
        this.cache = cache;
        this.ids = ids;
        this.publisher = publisher;
        this.props = props;
        this.clock = clock;
    }

    // --- leitura -----------------------------------------------------------------------------

    /** GET /produtos: só ativos, ordem alfabética, array completo. */
    public List<ProdutoDto> listar(String busca, String status) {
        if (status != null && !status.isBlank() && !StatusEstoque.valido(status)) {
            throw new ValidacaoException("Status inválido",
                    List.of(new CampoErro("status", "Use ok, atencao, baixo ou critico")));
        }
        String termo = busca == null || busca.isBlank() ? null : busca.trim();
        String termoNorm = termo == null ? null : normalizar(termo);
        return catalogo().stream()
                .filter(p -> status == null || status.isBlank() || status.equals(p.status()))
                .filter(p -> termo == null
                        || p.codigo().equalsIgnoreCase(termo)
                        || p.ncm().equals(termo)
                        || normalizar(p.nome()).contains(termoNorm)
                        || normalizar(p.codigo()).contains(termoNorm))
                .sorted(Comparator.comparing(ProdutoDto::nome, ALFABETICA))
                .toList();
    }

    public ProdutoDto buscar(long id) {
        return cache.produto(id).orElseGet(() -> {
            ProdutoItem p = ativo(id);
            ProdutoDto dto = ProdutoDto.de(p, repo.estoque(id).orElse(null));
            cache.guardar(dto);
            return dto;
        });
    }

    public ResumoEstoque resumo() {
        return cache.resumo(() -> {
            Map<Long, ProdutoItem> produtos = ativos();
            long baixo = 0;
            long critico = 0;
            BigDecimal valor = BigDecimal.ZERO;
            for (EstoqueItem e : repo.todoEstoque()) {
                ProdutoItem p = produtos.get(e.getProdutoId());
                if (p == null) {
                    continue;
                }
                if (StatusEstoque.CRITICO.equals(e.getStatus())) {
                    critico++;
                    baixo++;
                } else if (StatusEstoque.BAIXO.equals(e.getStatus())) {
                    baixo++;
                }
                valor = valor.add(p.getPrecoCusto().multiply(BigDecimal.valueOf(e.getEstoqueAtual())));
            }
            return new ResumoEstoque(produtos.size(), baixo, valor.setScale(2, RoundingMode.HALF_UP), critico);
        });
    }

    public List<Movimento> movimentos(long id, String de, String ate, String tipo) {
        repo.produto(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
        return repo.movimentos(id).stream()
                .filter(m -> tipo == null || tipo.isBlank() || tipo.equals(m.getTipo()))
                .filter(m -> de == null || de.isBlank() || m.getData().substring(0, 10).compareTo(de) >= 0)
                .filter(m -> ate == null || ate.isBlank() || m.getData().substring(0, 10).compareTo(ate) <= 0)
                .map(m -> new Movimento(m.getId(), m.getProdutoId(), m.getTipo(), m.getQuantidade(),
                        m.getEstoqueAnterior(), m.getEstoqueNovo(), m.getReferenciaId(), m.getReferenciaType(),
                        m.getUsuarioId(), m.getMotivo(), m.getData()))
                .toList();
    }

    public ProdutoFiscal fiscal(long id) {
        ProdutoItem p = repo.produto(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
        EstoqueItem e = repo.estoque(id).orElseGet(EstoqueItem::new);
        long atual = nz(e.getEstoqueAtual());
        long reservado = nz(e.getReservado());
        return new ProdutoFiscal(p.getId(), p.getCodigo(), p.getNome(), p.getNcm(), p.getPrecoVenda(),
                Boolean.TRUE.equals(p.getAtivo()), atual, reservado, atual - reservado, p.getCfop(), p.getOrigem(),
                p.getCstIcms(), p.getAliquotaIcms());
    }

    /** Disponibilidade somando linhas do mesmo produto (usada pelo sales-service). */
    public List<Disponibilidade> disponibilidade(List<ItemDisponibilidade> itens) {
        Map<Long, Long> pedido = new LinkedHashMap<>();
        for (ItemDisponibilidade i : itens) {
            pedido.merge(i.produtoId(), i.quantidade(), Long::sum);
        }
        return pedido.entrySet().stream().map(en -> {
            var p = repo.produto(en.getKey());
            if (p.isEmpty()) {
                return new Disponibilidade(en.getKey(), false, false, 0, false);
            }
            EstoqueItem e = repo.estoque(en.getKey()).orElseGet(EstoqueItem::new);
            long disp = nz(e.getEstoqueAtual()) - nz(e.getReservado());
            boolean ativo = Boolean.TRUE.equals(p.get().getAtivo());
            return new Disponibilidade(en.getKey(), true, ativo, disp, ativo && disp >= en.getValue());
        }).toList();
    }

    // --- escrita -----------------------------------------------------------------------------

    public ProdutoDto criar(ProdutoInput in, UsuarioAutenticado usuario) {
        validarPrecos(in);
        String codigo = in.codigo().trim();
        repo.porCodigo(codigo).ifPresent(x -> {
            throw new ConflitoException("Código já cadastrado");
        });
        String agora = clock.instant().toString();
        ProdutoItem p = new ProdutoItem();
        p.setId(ids.proximo("produtos"));
        aplicar(p, in);
        var fiscal = props.getFiscalPadrao();
        p.setCfop(fiscal.getCfop());
        p.setOrigem(fiscal.getOrigem());
        p.setCstIcms(fiscal.getCstIcms());
        p.setAliquotaIcms(fiscal.getAliquotaIcms());
        p.setAtivo(true);
        p.setDataCriacao(agora);
        p.setDataAtualizacao(agora);

        EstoqueItem e = new EstoqueItem();
        e.setProdutoId(p.getId());
        e.setEstoqueAtual(in.estoqueAtual());
        e.setReservado(0L);
        e.setDisponivel(in.estoqueAtual());
        e.setEstoqueMinimo(in.estoqueMinimo());
        e.setStatus(StatusEstoque.calcular(in.estoqueAtual(), in.estoqueMinimo()));
        e.setVersao(0L);
        e.setUltimaMovimentacao(agora);

        MovimentoItem inicial = null;
        if (in.estoqueAtual() > 0) {
            inicial = new MovimentoItem();
            inicial.setId(ids.proximo("movimentos"));
            inicial.setProdutoId(p.getId());
            inicial.setTipo("ajuste");
            inicial.setQuantidade(in.estoqueAtual());
            inicial.setEstoqueAnterior(0L);
            inicial.setEstoqueNovo(in.estoqueAtual());
            inicial.setReferenciaType("adjustment");
            inicial.setUsuarioId(usuario.id());
            inicial.setMotivo("Estoque inicial");
            inicial.setData(agora);
        }
        repo.criar(p, e, inicial);
        cache.invalidar(p.getId());
        return ProdutoDto.de(p, e);
    }

    public ProdutoDto atualizar(long id, ProdutoInput in, UsuarioAutenticado usuario) {
        validarPrecos(in);
        for (int tentativa = 1; ; tentativa++) {
            try {
                return atualizarUmaVez(id, in, usuario);
            } catch (VersaoDesatualizadaException e) {
                if (tentativa >= 3) {
                    throw new ConflitoException("O estoque mudou durante a edição. Tente novamente.");
                }
            }
        }
    }

    private ProdutoDto atualizarUmaVez(long id, ProdutoInput in, UsuarioAutenticado usuario) {
        ProdutoItem antigo = ativo(id);
        EstoqueItem est = repo.estoque(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
        String codigo = in.codigo().trim();
        repo.porCodigo(codigo).filter(o -> o.getId() != id).ifPresent(x -> {
            throw new ConflitoException("Código já cadastrado");
        });

        ProdutoItem novo = copia(antigo);
        aplicar(novo, in);
        novo.setDataAtualizacao(clock.instant().toString());

        EstoqueItem base = copia(est);
        base.setEstoqueMinimo(in.estoqueMinimo());
        boolean mudouQuantidade = in.estoqueAtual() != nz(est.getEstoqueAtual());
        boolean mudouMinimo = in.estoqueMinimo() != nz(est.getEstoqueMinimo());

        EstoqueService.Resultado resultado = null;
        EstoqueItem estoqueNovo = null;
        MovimentoItem mov = null;
        if (mudouQuantidade) {
            UsuarioAtual.exigir(Papel.ADMIN); // só ADMIN altera quantidade (A10)
            try {
                resultado = estoque.calcular(base, new Operacao(Operacao.ADJUST, id, in.estoqueAtual(), null,
                        "adjustment", usuario.id(), MOTIVO_TELA));
            } catch (EstoqueException e) {
                throw new RegraNegocioException("Quantidade menor que o reservado",
                        List.of(new CampoErro("estoqueAtual", "Menor que o reservado (" + est.getReservado() + ")")));
            }
            resultado = new EstoqueService.Resultado(est, resultado.depois(), resultado.movimento());
            estoqueNovo = resultado.depois();
            mov = resultado.movimento();
        } else if (mudouMinimo) {
            estoqueNovo = base;
            estoqueNovo.setStatus(StatusEstoque.calcular(nz(base.getEstoqueAtual()), in.estoqueMinimo()));
            estoqueNovo.setVersao(nz(est.getVersao()) + 1);
            resultado = new EstoqueService.Resultado(est, estoqueNovo, null);
        }
        repo.atualizar(antigo, novo, estoqueNovo, nz(est.getVersao()), mov);
        if (resultado != null) {
            estoque.depoisDeGravar(resultado);
        } else {
            cache.invalidar(id);
        }
        return ProdutoDto.de(novo, estoqueNovo != null ? estoqueNovo : est);
    }

    public void desativar(long id) {
        ProdutoItem p = repo.produto(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
        if (!Boolean.TRUE.equals(p.getAtivo())) {
            return;
        }
        if (nz(repo.estoque(id).map(EstoqueItem::getReservado).orElse(0L)) > 0) {
            throw new ConflitoException("Produto com estoque reservado");
        }
        p.setAtivo(false);
        p.setDataAtualizacao(clock.instant().toString());
        repo.salvarProduto(p);
        cache.invalidar(id);
    }

    /** POST /produtos/{id}/ajuste (extra): publica o adjust e responde 202. */
    public AjusteAceito ajusteAssincrono(long id, AjusteInput in, UsuarioAutenticado usuario) {
        String motivo = in.motivo().trim();
        if (motivo.length() < 10) {
            throw new ValidacaoException("Motivo deve ter pelo menos 10 caracteres",
                    List.of(new CampoErro("motivo", "Mínimo de 10 caracteres")));
        }
        ativo(id);
        long reservado = nz(repo.estoque(id).map(EstoqueItem::getReservado).orElse(0L));
        if (in.quantidadeNova() < reservado) {
            throw new RegraNegocioException("Quantidade menor que o reservado");
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", Operacao.ADJUST);
        msg.put("produtoId", id);
        msg.put("quantidade", in.quantidadeNova());
        msg.put("referenciaType", "adjustment");
        msg.put("usuarioId", usuario.id());
        msg.put("motivo", motivo);
        msg.put("timestamp", clock.instant().toString());
        publisher.enviarFila(props.getFilaEstoque(), Operacao.ADJUST, msg);
        return new AjusteAceito(id, in.quantidadeNova(), "processando");
    }

    // --- apoio -------------------------------------------------------------------------------

    private List<ProdutoDto> catalogo() {
        Map<Long, EstoqueItem> est = repo.todoEstoque().stream()
                .collect(Collectors.toMap(EstoqueItem::getProdutoId, Function.identity()));
        return ativos().values().stream().map(p -> ProdutoDto.de(p, est.get(p.getId()))).toList();
    }

    private Map<Long, ProdutoItem> ativos() {
        return repo.todosProdutos().stream().filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .collect(Collectors.toMap(ProdutoItem::getId, Function.identity()));
    }

    private ProdutoItem ativo(long id) {
        return repo.produto(id).filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
    }

    private static void validarPrecos(ProdutoInput in) {
        if (in.precoVenda().compareTo(in.precoCusto()) <= 0) {
            throw new ValidacaoException("Preço de venda deve ser maior que o preço de custo",
                    List.of(new CampoErro("precoVenda", "Deve ser maior que o preço de custo")));
        }
    }

    private static void aplicar(ProdutoItem p, ProdutoInput in) {
        p.setCodigo(in.codigo().trim());
        p.setNome(in.nome().trim());
        p.setNomeBusca(normalizar(p.getNome()));
        p.setNcm(in.ncm());
        p.setEstoqueMinimo(in.estoqueMinimo());
        p.setPrecoCusto(in.precoCusto().setScale(2, RoundingMode.HALF_UP));
        p.setPrecoVenda(in.precoVenda().setScale(2, RoundingMode.HALF_UP));
    }

    private static ProdutoItem copia(ProdutoItem o) {
        ProdutoItem p = new ProdutoItem();
        p.setId(o.getId());
        p.setCfop(o.getCfop());
        p.setOrigem(o.getOrigem());
        p.setCstIcms(o.getCstIcms());
        p.setAliquotaIcms(o.getAliquotaIcms());
        p.setAtivo(o.getAtivo());
        p.setDataCriacao(o.getDataCriacao());
        return p;
    }

    private static EstoqueItem copia(EstoqueItem o) {
        EstoqueItem e = new EstoqueItem();
        e.setProdutoId(o.getProdutoId());
        e.setEstoqueAtual(o.getEstoqueAtual());
        e.setReservado(o.getReservado());
        e.setDisponivel(o.getDisponivel());
        e.setEstoqueMinimo(o.getEstoqueMinimo());
        e.setStatus(o.getStatus());
        e.setVersao(o.getVersao());
        e.setUltimaMovimentacao(o.getUltimaMovimentacao());
        return e;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
