package br.com.wcautopecas.fiscal.emissao;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.integracao.Integracoes;
import br.com.wcautopecas.fiscal.integracao.Integracoes.Cliente;
import br.com.wcautopecas.fiscal.integracao.Integracoes.ProdutoFiscal;
import br.com.wcautopecas.fiscal.integracao.Integracoes.Venda;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.Numerador;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** HU-B07.1: venda finalizada vira nota "processando" com itens, número e job de XML. */
@Service
public class CriarNotaDeVenda {

    private static final Logger log = LoggerFactory.getLogger(CriarNotaDeVenda.class);

    private final NotaRepository repo;
    private final Integracoes integracoes;
    private final Numerador numerador;
    private final IdGenerator ids;
    private final FiscalMensagens mensagens;
    private final FiscalProperties props;
    private final Clock clock;

    public CriarNotaDeVenda(NotaRepository repo, Integracoes integracoes, Numerador numerador, IdGenerator ids,
            FiscalMensagens mensagens, FiscalProperties props, Clock clock) {
        this.repo = repo;
        this.integracoes = integracoes;
        this.numerador = numerador;
        this.ids = ids;
        this.mensagens = mensagens;
        this.props = props;
        this.clock = clock;
    }

    /** Idempotente: se a venda já tem nota, só garante o vínculo e o job (se ainda sem XML). */
    public NotaItem criar(long vendaId) {
        Optional<NotaItem> existente = repo.daVenda(vendaId).stream().findFirst();
        if (existente.isPresent()) {
            NotaItem n = existente.get();
            log.info("venda {} já tem a nota {}", vendaId, n.getId());
            integracoes.vincularNota(vendaId, n.getId());
            if (n.getXmlPath() == null && "processando".equals(n.getStatus())) {
                mensagens.gerarXml(n);
            }
            return n;
        }

        Venda venda = integracoes.venda(vendaId);
        Cliente cliente = venda.clienteId() == null ? null : integracoes.cliente(venda.clienteId());
        Map<Long, ProdutoFiscal> produtos = new HashMap<>();
        venda.itens().forEach(i -> produtos.computeIfAbsent(i.produtoId(), integracoes::produto));

        NotaItem n = new NotaItem();
        n.setTipo("nfe".equals(venda.tipoDocumento()) ? "NFE" : "NFCE");
        n.setModelo(Numerador.modelo(n.getTipo()));
        n.setSerie(props.getSerie());
        n.setId(ids.proximo("notas"));
        n.setNumero(numerador.proximo(n.getModelo(), n.getSerie()));
        n.setStatus("processando");
        n.setVendaId(vendaId);
        if (cliente != null) {
            n.setClienteId(cliente.id());
            n.setClienteNome(cliente.nome());
            n.setClienteDocumento(cliente.documento());
            n.setClienteUf(vazioComoNull(cliente.uf()));
        }
        n.setValorProdutos(venda.subtotal());
        n.setValorDesconto(venda.desconto());
        n.setValorTotal(venda.total());
        n.setEmitidaEm(clock.instant().toString());
        n.setAmbiente(props.getAmbiente());
        n.setTentativasEnvio(0);

        boolean interestadual = n.getClienteUf() != null && !n.getClienteUf().equalsIgnoreCase(props.getUfEmitente());
        List<NotaLinha> linhas = new ArrayList<>();
        long seq = 1;
        for (var i : venda.itens()) {
            ProdutoFiscal p = produtos.get(i.produtoId());
            NotaLinha l = new NotaLinha();
            l.setNotaId(n.getId());
            l.setItemId(seq++);
            l.setProdutoId(i.produtoId());
            l.setCodigo(p.codigo());
            l.setDescricao(p.nome());
            l.setNcm(p.ncm());
            l.setCfop(cfopVenda(p.cfop(), interestadual));
            l.setOrigem(p.origem());
            l.setCstIcms(p.cstIcms());
            l.setAliquotaIcms(p.aliquotaIcms());
            l.setQuantidade(i.quantidade());
            l.setValorUnitario(i.valorUnitario());
            l.setQuantidadeDevolvida(0L);
            l.setQuantidadeEmDevolucao(0L);
            linhas.add(l);
        }
        repo.criar(n, linhas, List.of());
        log.info("nota {} {} {} criada para a venda {}", n.getId(), n.getTipo(), n.getNumero(), vendaId);
        integracoes.vincularNota(vendaId, n.getId());
        mensagens.gerarXml(n);
        return n;
    }

    /** CFOP 5xxx dentro do estado; para cliente de outra UF, o 6xxx equivalente. */
    static String cfopVenda(String cfop, boolean interestadual) {
        String base = cfop == null || cfop.isBlank() ? "5102" : cfop;
        return interestadual && base.startsWith("5") ? "6" + base.substring(1) : base;
    }

    private static String vazioComoNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
