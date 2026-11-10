package br.com.wcautopecas.fiscal.nota;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.api.DevolucaoInput;
import br.com.wcautopecas.fiscal.api.NotaDto;
import br.com.wcautopecas.fiscal.emissao.FiscalMensagens;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

/**
 * HU-B07.7: nota de devolução (modelo 55) referenciando uma nota de venda autorizada. O saldo é
 * por produto; a quantidade fica "em devolução" até a autorização (EPB-09), para não devolver
 * duas vezes em paralelo. Um lock no Redis por nota original serializa os pedidos.
 */
@Service
public class DevolucaoService {

    static final Set<String> TIPOS_VENDA = Set.of("NFE", "NFCE");
    static final String MOTIVO_PADRAO = "Devolução de mercadoria";

    private final NotaRepository repo;
    private final NotaService notas;
    private final Numerador numerador;
    private final IdGenerator ids;
    private final IdempotencyGuard locks;
    private final FiscalMensagens mensagens;
    private final FiscalProperties props;
    private final Clock clock;

    public DevolucaoService(NotaRepository repo, NotaService notas, Numerador numerador, IdGenerator ids,
            IdempotencyGuard locks, FiscalMensagens mensagens, FiscalProperties props, Clock clock) {
        this.repo = repo;
        this.notas = notas;
        this.numerador = numerador;
        this.ids = ids;
        this.locks = locks;
        this.mensagens = mensagens;
        this.props = props;
        this.clock = clock;
    }

    public NotaDto devolver(DevolucaoInput in) {
        Map<Long, Long> pedidas = new LinkedHashMap<>();
        in.itens().forEach(i -> {
            if (i.quantidade() != null && i.quantidade() > 0) {
                pedidas.merge(i.produtoId(), i.quantidade(), Long::sum);
            }
        });
        if (pedidas.isEmpty()) {
            throw NotaService.invalido("itens", "Informe pelo menos um item com quantidade a devolver");
        }
        NotaItem original = notas.existente(in.notaReferenciadaId());
        if (!TIPOS_VENDA.contains(original.getTipo())) {
            throw new RegraNegocioException("Só notas de venda podem ser devolvidas");
        }
        if (!"autorizada".equals(original.getStatus())) {
            throw new RegraNegocioException("A nota original precisa estar autorizada");
        }

        String lock = "devolucao:" + original.getId();
        if (!locks.primeiraVez(lock, Duration.ofSeconds(30))) {
            throw new ConflitoException("Outra devolução desta nota está em andamento. Tente de novo");
        }
        try {
            return criar(original, pedidas, in.motivo());
        } finally {
            locks.liberar(lock);
        }
    }

    private NotaDto criar(NotaItem original, Map<Long, Long> pedidas, String motivo) {
        List<NotaLinha> linhasOriginais = repo.itens(original.getId());
        Map<Long, NotaLinha> primeira = new LinkedHashMap<>();
        Map<Long, Long> vendida = new LinkedHashMap<>();
        for (NotaLinha l : linhasOriginais) {
            primeira.putIfAbsent(l.getProdutoId(), l);
            vendida.merge(l.getProdutoId(), l.getQuantidade(), Long::sum);
        }
        for (var e : pedidas.entrySet()) {
            NotaLinha base = primeira.get(e.getKey());
            if (base == null) {
                throw new RegraNegocioException("Produto não consta na nota original");
            }
            long saldo = vendida.get(e.getKey()) - nz(base.getQuantidadeDevolvida()) - nz(base.getQuantidadeEmDevolucao());
            if (e.getValue() > saldo) {
                throw new RegraNegocioException("Quantidade acima do saldo de " + base.getCodigo()
                        + ": pode devolver até " + saldo);
            }
        }

        NotaItem n = new NotaItem();
        n.setTipo("DEVOLUCAO");
        n.setModelo("55");
        n.setSerie(props.getSerie());
        n.setId(ids.proximo("notas"));
        n.setNumero(numerador.proximo(n.getModelo(), n.getSerie()));
        n.setStatus("processando");
        n.setClienteId(original.getClienteId());
        n.setClienteNome(original.getClienteNome());
        n.setClienteDocumento(original.getClienteDocumento());
        n.setClienteUf(original.getClienteUf());
        n.setNotaReferenciadaId(original.getId());
        n.setChaveReferenciada(original.getChaveAcesso());
        n.setMotivo(motivo == null || motivo.isBlank() ? MOTIVO_PADRAO : motivo.trim());
        n.setEmitidaEm(clock.instant().toString());
        n.setAmbiente(props.getAmbiente());
        n.setTentativasEnvio(0);

        boolean interestadual = n.getClienteUf() != null && !n.getClienteUf().equalsIgnoreCase(props.getUfEmitente());
        List<NotaLinha> linhas = new ArrayList<>();
        List<TransactWriteItem> extras = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long seq = 1;
        for (var e : pedidas.entrySet()) {
            NotaLinha base = primeira.get(e.getKey());
            NotaLinha l = new NotaLinha();
            l.setNotaId(n.getId());
            l.setItemId(seq++);
            l.setProdutoId(base.getProdutoId());
            l.setCodigo(base.getCodigo());
            l.setDescricao(base.getDescricao());
            l.setNcm(base.getNcm());
            l.setCfop(interestadual ? "2202" : "1202");
            l.setOrigem(base.getOrigem());
            l.setCstIcms(base.getCstIcms());
            l.setAliquotaIcms(base.getAliquotaIcms());
            l.setQuantidade(e.getValue());
            l.setValorUnitario(base.getValorUnitario());
            l.setQuantidadeDevolvida(0L);
            l.setQuantidadeEmDevolucao(0L);
            linhas.add(l);
            total = total.add(base.getValorUnitario().multiply(BigDecimal.valueOf(e.getValue())));

            base.setQuantidadeEmDevolucao(nz(base.getQuantidadeEmDevolucao()) + e.getValue());
            extras.add(repo.linhaTx(base));
        }
        total = total.setScale(2, RoundingMode.HALF_EVEN);
        n.setValorProdutos(total);
        n.setValorDesconto(BigDecimal.ZERO.setScale(2));
        n.setValorTotal(total);
        repo.criar(n, linhas, extras);
        mensagens.gerarXml(n);
        return NotaDto.de(n);
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
