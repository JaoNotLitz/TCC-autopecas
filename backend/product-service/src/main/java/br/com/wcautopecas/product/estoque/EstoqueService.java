package br.com.wcautopecas.product.estoque;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.product.ProdutosProperties;
import br.com.wcautopecas.product.produto.EstoqueItem;
import br.com.wcautopecas.product.produto.ProdutoCache;
import br.com.wcautopecas.product.produto.ProdutoItem;
import br.com.wcautopecas.product.produto.ProdutoRepository;
import br.com.wcautopecas.product.produto.ProdutoRepository.VersaoDesatualizadaException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Regras do estoque (EPB-05): aplica reserve/release/decrease/increase/adjust com controle
 * otimista por {@code versao}, grava a movimentação na mesma transação, recalcula o status e
 * publica o alerta quando ele muda para baixo ou crítico. Usado pelo listener da fila e pelo
 * PUT de produto quando a tela altera a quantidade.
 */
@Service
public class EstoqueService {

    private static final Logger log = LoggerFactory.getLogger(EstoqueService.class);
    private static final int TENTATIVAS = 5;

    private final ProdutoRepository repo;
    private final IdGenerator ids;
    private final ProdutoCache cache;
    private final EventPublisher publisher;
    private final ProdutosProperties props;
    private final Clock clock;

    public EstoqueService(ProdutoRepository repo, IdGenerator ids, ProdutoCache cache, EventPublisher publisher,
            ProdutosProperties props, Clock clock) {
        this.repo = repo;
        this.ids = ids;
        this.cache = cache;
        this.publisher = publisher;
        this.props = props;
        this.clock = clock;
    }

    /** Resultado de uma operação aplicada. */
    public record Resultado(EstoqueItem antes, EstoqueItem depois, MovimentoItem movimento) {
    }

    public Resultado aplicar(Operacao op) {
        validar(op);
        for (int tentativa = 1; ; tentativa++) {
            EstoqueItem atual = repo.estoque(op.produtoId())
                    .orElseThrow(() -> new EstoqueException("PRODUTO_INEXISTENTE",
                            "produto " + op.produtoId() + " sem registro de estoque"));
            Resultado r = calcular(atual, op);
            try {
                repo.gravarMovimento(r.depois(), versao(atual), r.movimento());
            } catch (VersaoDesatualizadaException e) {
                if (tentativa >= TENTATIVAS) {
                    throw e; // a mensagem volta para a fila
                }
                continue;
            }
            depoisDeGravar(r);
            return r;
        }
    }

    /** Calcula o novo estado sem gravar (também usado pelo PUT de produto). */
    public Resultado calcular(EstoqueItem atual, Operacao op) {
        long estoqueAtual = valor(atual.getEstoqueAtual());
        long reservado = valor(atual.getReservado());
        long q = op.quantidade();
        long novoAtual = estoqueAtual;
        long novoReservado = reservado;
        String tipoMov;
        long qtdMov;
        switch (op.type()) {
            case Operacao.RESERVE -> {
                if (estoqueAtual - reservado < q) {
                    throw new EstoqueException("ESTOQUE_INSUFICIENTE", "produto " + op.produtoId() + " tem "
                            + (estoqueAtual - reservado) + " disponível e a reserva pede " + q);
                }
                novoReservado = reservado + q;
                tipoMov = "reserva";
                qtdMov = -q;
            }
            case Operacao.RELEASE -> {
                novoReservado = Math.max(0, reservado - q);
                tipoMov = "liberacao";
                qtdMov = reservado - novoReservado;
            }
            case Operacao.DECREASE -> {
                if (q > estoqueAtual) {
                    throw new EstoqueException("ESTOQUE_NEGATIVO", "baixa de " + q + " com estoque " + estoqueAtual
                            + " no produto " + op.produtoId());
                }
                if (reservado < q) {
                    log.warn("baixa de {} no produto {} sem reserva correspondente (reservado {})", q,
                            op.produtoId(), reservado);
                }
                novoAtual = estoqueAtual - q;
                novoReservado = Math.max(0, reservado - q);
                tipoMov = "saida";
                qtdMov = -q;
            }
            case Operacao.INCREASE -> {
                novoAtual = estoqueAtual + q;
                tipoMov = "entrada";
                qtdMov = q;
            }
            case Operacao.ADJUST -> {
                if (q < reservado) {
                    throw new EstoqueException("ABAIXO_DO_RESERVADO", "Quantidade menor que o reservado");
                }
                novoAtual = q;
                tipoMov = "ajuste";
                qtdMov = q - estoqueAtual;
            }
            default -> throw new EstoqueException("TIPO_DESCONHECIDO", "type " + op.type());
        }
        String agora = clock.instant().toString();
        EstoqueItem depois = new EstoqueItem();
        depois.setProdutoId(atual.getProdutoId());
        depois.setEstoqueAtual(novoAtual);
        depois.setReservado(novoReservado);
        depois.setDisponivel(novoAtual - novoReservado);
        depois.setEstoqueMinimo(atual.getEstoqueMinimo());
        depois.setStatus(StatusEstoque.calcular(novoAtual, valor(atual.getEstoqueMinimo())));
        depois.setVersao(versao(atual) + 1);
        depois.setUltimaMovimentacao(agora);

        MovimentoItem m = new MovimentoItem();
        m.setId(ids.proximo("movimentos"));
        m.setProdutoId(atual.getProdutoId());
        m.setTipo(tipoMov);
        m.setQuantidade(qtdMov);
        m.setEstoqueAnterior(estoqueAtual);
        m.setEstoqueNovo(novoAtual);
        m.setReferenciaId(op.referenciaId());
        m.setReferenciaType(op.referenciaType());
        m.setUsuarioId(op.usuarioId());
        m.setMotivo(op.motivo());
        m.setData(agora);
        return new Resultado(atual, depois, m);
    }

    /** Cache e alerta depois que a gravação deu certo. */
    public void depoisDeGravar(Resultado r) {
        cache.invalidar(r.depois().getProdutoId());
        String antes = r.antes().getStatus();
        String depois = r.depois().getStatus();
        if (!depois.equals(antes) && (StatusEstoque.CRITICO.equals(depois) || StatusEstoque.BAIXO.equals(depois))) {
            alertar(r.depois());
        }
    }

    /** Alerta da seção 7.1, com o atributo {@code status} usado na filter policy da email-queue. */
    void alertar(EstoqueItem e) {
        ProdutoItem p = repo.produto(e.getProdutoId()).orElse(null);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("MessageType", "low_stock_alert");
        msg.put("produtoId", e.getProdutoId());
        msg.put("codigo", p == null ? null : p.getCodigo());
        msg.put("nome", p == null ? null : p.getNome());
        msg.put("estoqueAtual", e.getEstoqueAtual());
        msg.put("estoqueMinimo", e.getEstoqueMinimo());
        msg.put("status", e.getStatus());
        msg.put("timestamp", clock.instant().toString());
        try {
            publisher.publicarTopico(props.getTopicoAlertas(), "low_stock_alert", msg,
                    Map.of("status", e.getStatus()));
        } catch (Exception ex) {
            log.error("falha ao publicar alerta de estoque do produto {}", e.getProdutoId(), ex);
        }
    }

    /** Aviso operacional (estoque insuficiente na reserva assíncrona). */
    public void avisarSistema(String mensagem) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("MessageType", "warning");
        msg.put("service", "product-service");
        msg.put("severity", "high");
        msg.put("message", mensagem);
        msg.put("timestamp", clock.instant().toString());
        try {
            publisher.publicarTopico(props.getTopicoSistema(), "warning", msg, Map.of("severity", "high"));
        } catch (Exception ex) {
            log.error("falha ao publicar aviso de sistema", ex);
        }
    }

    private static void validar(Operacao op) {
        if (op.type() == null || op.produtoId() == null || op.quantidade() == null) {
            throw new EstoqueException("MENSAGEM_INVALIDA", "type, produtoId e quantidade são obrigatórios");
        }
        boolean ajuste = Operacao.ADJUST.equals(op.type());
        if (ajuste ? op.quantidade() < 0 : op.quantidade() <= 0) {
            throw new EstoqueException("MENSAGEM_INVALIDA", "quantidade inválida: " + op.quantidade());
        }
    }

    private static long valor(Long v) {
        return v == null ? 0 : v;
    }

    private static long versao(EstoqueItem e) {
        return e.getVersao() == null ? 0 : e.getVersao();
    }
}
