package br.com.wcautopecas.sales.venda;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.sales.api.VendaDto;
import br.com.wcautopecas.sales.api.VendaInput;
import br.com.wcautopecas.sales.integracao.Integracoes;
import br.com.wcautopecas.sales.integracao.Integracoes.Cliente;
import br.com.wcautopecas.sales.integracao.Integracoes.Produto;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/** Regras das HU-B06.1 a B06.7. */
@Service
public class VendaService {

    public static final String ORCAMENTO = "orcamento";
    public static final String FINALIZADA = "finalizada";
    public static final String CANCELADA = "cancelada";

    private final VendaRepository repo;
    private final Integracoes integracoes;
    private final VendaEventos eventos;
    private final IdGenerator ids;
    private final Clock clock;

    public VendaService(VendaRepository repo, Integracoes integracoes, VendaEventos eventos, IdGenerator ids,
            Clock clock) {
        this.repo = repo;
        this.integracoes = integracoes;
        this.eventos = eventos;
        this.ids = ids;
        this.clock = clock;
    }

    public VendaDto criar(VendaInput in, UsuarioAutenticado usuario) {
        Pagamento.validar(in.metodoPagamento(), in.bandeira(), in.destinoDinheiro());
        var valores = CalculadoraVenda.calcular(in.itens(), in.desconto());
        Cliente cliente = clienteValido(in.tipoDocumento(), in.clienteId());
        Map<Long, Produto> produtos = validarEstoque(in.itens());

        VendaItem v = new VendaItem();
        v.setId(ids.proximo("vendas"));
        v.setNumero(String.format("VD-%06d", v.getId()));
        v.setTipoDocumento(in.tipoDocumento());
        v.setModalidade(in.modalidade());
        v.setClienteId(in.clienteId());
        v.setMetodoPagamento(in.metodoPagamento());
        v.setBandeira(vazioComoNull(in.bandeira()));
        v.setDestinoDinheiro(vazioComoNull(in.destinoDinheiro()));
        v.setDesconto(valores.desconto());
        v.setSubtotal(valores.subtotal());
        v.setTotal(valores.total());
        v.setStatus(in.status());
        v.setVendedorId(usuario.id());
        v.setData(clock.instant().toString());
        if (FINALIZADA.equals(in.status())) {
            v.setFinalizadaEm(v.getData());
        }

        List<VendaItemLinha> linhas = new ArrayList<>();
        long n = 1;
        for (VendaInput.Item i : in.itens()) {
            Produto p = produtos.get(i.produtoId());
            VendaItemLinha l = new VendaItemLinha();
            l.setVendaId(v.getId());
            l.setItemId(n++);
            l.setProdutoId(i.produtoId());
            l.setCodigo(p.codigo());
            l.setNome(p.nome());
            l.setQuantidade(i.quantidade());
            l.setValorUnitario(CalculadoraVenda.dinheiro(i.valorUnitario()));
            linhas.add(l);
        }
        repo.criar(v, linhas);

        eventos.reservar(v, linhas, usuario.id());
        if (FINALIZADA.equals(v.getStatus())) {
            eventos.vendaFinalizada(v, cliente);
        }
        return VendaDto.de(v, linhas);
    }

    public List<VendaDto.Detalhe> listar(String status, Long clienteId, String de, String ate) {
        String inicio = de == null || de.isBlank() ? null : data(de, "de").toString();
        String fim = ate == null || ate.isBlank() ? null : data(ate, "ate").plusDays(1).toString();
        Stream<VendaItem> fonte;
        if (clienteId != null) {
            fonte = repo.doCliente(clienteId).stream();
        } else if (status != null && !status.isBlank()) {
            fonte = repo.doStatus(status).stream();
        } else {
            fonte = repo.todas().stream();
        }
        return fonte
                .filter(v -> status == null || status.isBlank() || status.equals(v.getStatus()))
                .filter(v -> inicio == null || v.getData().compareTo(inicio) >= 0)
                .filter(v -> fim == null || v.getData().compareTo(fim) < 0)
                .sorted(Comparator.comparing(VendaItem::getData).reversed())
                .map(v -> VendaDto.detalhe(v, repo.itens(v.getId())))
                .toList();
    }

    public VendaDto.Detalhe buscar(long id) {
        VendaItem v = existente(id);
        return VendaDto.detalhe(v, repo.itens(id));
    }

    /** HU-B06.6: só orçamento muda de status; venda finalizada se cancela pela nota fiscal. */
    public VendaDto.Detalhe alterarStatus(long id, String novo, UsuarioAutenticado usuario) {
        if (!FINALIZADA.equals(novo) && !CANCELADA.equals(novo)) {
            throw new ValidacaoException("Status deve ser finalizada ou cancelada",
                    List.of(new CampoErro("status", "Status deve ser finalizada ou cancelada")));
        }
        VendaItem v = existente(id);
        if (FINALIZADA.equals(v.getStatus())) {
            throw new RegraNegocioException("Vendas finalizadas devem ser canceladas pela nota fiscal");
        }
        if (CANCELADA.equals(v.getStatus())) {
            throw new RegraNegocioException("Venda já está cancelada");
        }
        List<VendaItemLinha> linhas = repo.itens(id);
        Cliente cliente = null;
        String agora = clock.instant().toString();
        if (FINALIZADA.equals(novo)) {
            cliente = clienteValido(v.getTipoDocumento(), v.getClienteId());
            v.setFinalizadaEm(agora);
        } else {
            v.setCanceladaEm(agora);
        }
        v.setStatus(novo);
        if (!repo.salvarSeStatus(v, ORCAMENTO)) {
            throw new ConflitoException("A venda foi alterada por outra operação. Atualize e tente de novo");
        }
        if (FINALIZADA.equals(novo)) {
            eventos.vendaFinalizada(v, cliente);
        } else {
            eventos.liberar(v, linhas, usuario.id());
        }
        return VendaDto.detalhe(v, linhas);
    }

    /** HU-B06.7: chamado pelo fiscal-service quando cria a nota da venda. */
    public void vincularNota(long vendaId, long notaFiscalId) {
        try {
            repo.vincularNota(vendaId, notaFiscalId);
        } catch (ConditionalCheckFailedException e) {
            throw new NaoEncontradoException("Venda não encontrada");
        }
    }

    /**
     * EPB-10: a nota da venda foi cancelada na SEFAZ. A venda passa para "cancelada"; o estoque
     * volta pelo fiscal-service ({@code increase}), então aqui não há {@code release}. Idempotente.
     */
    public void canceladaPelaNota(long vendaId, long notaFiscalId) {
        VendaItem v = existente(vendaId);
        if (CANCELADA.equals(v.getStatus())) {
            return;
        }
        if (!FINALIZADA.equals(v.getStatus())) {
            throw new RegraNegocioException("Só vendas finalizadas são canceladas pela nota fiscal");
        }
        v.setStatus(CANCELADA);
        v.setCanceladaEm(clock.instant().toString());
        if (v.getNotaFiscalId() == null) {
            v.setNotaFiscalId(notaFiscalId);
        }
        if (!repo.salvarSeStatus(v, FINALIZADA)) {
            throw new ConflitoException("A venda foi alterada por outra operação. Atualize e tente de novo");
        }
    }

    private VendaItem existente(long id) {
        return repo.porId(id).orElseThrow(() -> new NaoEncontradoException("Venda não encontrada"));
    }

    /** HU-B06.2: NF-e exige cliente; quando há cliente, ele precisa existir e estar ativo. */
    private Cliente clienteValido(String tipoDocumento, Long clienteId) {
        if (clienteId == null) {
            if ("nfe".equals(tipoDocumento)) {
                throw new ValidacaoException("NF-e exige cliente identificado",
                        List.of(new CampoErro("clienteId", "NF-e exige cliente identificado")));
            }
            return null;
        }
        Cliente c = integracoes.cliente(clienteId);
        if (!c.ativo()) {
            throw new RegraNegocioException("Cliente inativo");
        }
        return c;
    }

    /**
     * HU-B06.3: soma as linhas do mesmo produto e compara com o disponível (estoque − reservado).
     * A checagem não trava o estoque: a reserva é assíncrona e o listener recusa se faltar.
     */
    private Map<Long, Produto> validarEstoque(List<VendaInput.Item> itens) {
        Map<Long, Long> somas = new LinkedHashMap<>();
        Map<Long, Integer> primeiraLinha = new LinkedHashMap<>();
        for (int i = 0; i < itens.size(); i++) {
            somas.merge(itens.get(i).produtoId(), itens.get(i).quantidade(), Long::sum);
            primeiraLinha.putIfAbsent(itens.get(i).produtoId(), i);
        }
        Map<Long, Produto> produtos = new LinkedHashMap<>();
        for (var e : somas.entrySet()) {
            Produto p = integracoes.produto(e.getKey());
            if (!p.ativo()) {
                throw new NaoEncontradoException("Produto " + p.codigo() + " está inativo");
            }
            if (p.disponivel() < e.getValue()) {
                String msg = "Estoque insuficiente para " + p.codigo() + ": disponível " + p.disponivel();
                throw new ConflitoException(msg, List.of(new CampoErro(
                        "itens[" + primeiraLinha.get(e.getKey()) + "].quantidade", "Disponível: " + p.disponivel())));
            }
            produtos.put(e.getKey(), p);
        }
        return produtos;
    }

    private static LocalDate data(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new ValidacaoException("Data inválida (use AAAA-MM-DD)",
                    List.of(new CampoErro(campo, "Data inválida (use AAAA-MM-DD)")));
        }
    }

    private static String vazioComoNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
