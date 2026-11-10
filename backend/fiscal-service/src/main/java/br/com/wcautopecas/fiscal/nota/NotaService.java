package br.com.wcautopecas.fiscal.nota;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.api.NotaDto;
import br.com.wcautopecas.fiscal.emissao.FiscalMensagens;
import br.com.wcautopecas.fiscal.evento.EventoFiscal;
import br.com.wcautopecas.fiscal.evento.EventoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/** Consulta (HU-B07.2/B07.3), reenvio (B07.4) e cancelamento (B07.5). */
@Service
public class NotaService {

    public static final Set<String> STATUS = Set.of("processando", "autorizada", "cancelada", "rejeitada");
    public static final Set<String> TIPOS = Set.of("NFE", "NFCE", "DEVOLUCAO", "ENTRADA");
    static final int JUSTIFICATIVA_MINIMA = 15;
    static final java.time.ZoneId FUSO_LOJA = java.time.ZoneId.of("America/Sao_Paulo");

    private final NotaRepository repo;
    private final EventoRepository eventos;
    private final FiscalMensagens mensagens;
    private final IdGenerator ids;
    private final DynamoDbClient dynamo;
    private final FiscalProperties props;
    private final Clock clock;
    private final SaldoDevolucao saldo;

    public NotaService(NotaRepository repo, EventoRepository eventos, FiscalMensagens mensagens, IdGenerator ids,
            DynamoDbClient dynamo, FiscalProperties props, Clock clock, SaldoDevolucao saldo) {
        this.saldo = saldo;
        this.repo = repo;
        this.eventos = eventos;
        this.mensagens = mensagens;
        this.ids = ids;
        this.dynamo = dynamo;
        this.props = props;
        this.clock = clock;
    }

    public List<NotaDto> listar(String status, String de, String ate, String tipo, Long vendaId) {
        if (status != null && !status.isBlank() && !STATUS.contains(status)) {
            throw invalido("status", "Status inválido");
        }
        if (tipo != null && !tipo.isBlank() && !TIPOS.contains(tipo)) {
            throw invalido("tipo", "Tipo inválido");
        }
        // de/ate são dias no fuso da loja; emitidaEm é um instante UTC (uma venda às 22h ainda é "hoje").
        String inicio = de == null || de.isBlank() ? null : inicioDoDia(data(de, "de"));
        String fim = ate == null || ate.isBlank() ? null : inicioDoDia(data(ate, "ate").plusDays(1));
        Stream<NotaItem> fonte;
        if (vendaId != null) {
            fonte = repo.daVenda(vendaId).stream();
        } else if (status != null && !status.isBlank()) {
            fonte = repo.doStatus(status).stream();
        } else if (tipo != null && !tipo.isBlank()) {
            fonte = repo.doTipo(tipo).stream();
        } else {
            fonte = repo.todas().stream();
        }
        return fonte
                .filter(n -> vazio(status) || status.equals(n.getStatus()))
                .filter(n -> vazio(tipo) || tipo.equals(n.getTipo()))
                .filter(n -> inicio == null || n.getEmitidaEm().compareTo(inicio) >= 0)
                .filter(n -> fim == null || n.getEmitidaEm().compareTo(fim) < 0)
                .sorted(Comparator.comparing(NotaItem::getEmitidaEm).reversed())
                .map(NotaDto::de)
                .toList();
    }

    public NotaItem existente(long id) {
        return repo.porId(id).orElseThrow(() -> new NaoEncontradoException("Nota fiscal não encontrada"));
    }

    public List<NotaDto.Item> itens(long id) {
        existente(id);
        return repo.itens(id).stream().map(NotaDto.Item::de).toList();
    }

    /** HU-B07.4 (extra): nota rejeitada volta para "processando" e o XML é regerado. */
    public NotaDto reenviar(long id) {
        NotaItem n = existente(id);
        if (!"rejeitada".equals(n.getStatus())) {
            throw new RegraNegocioException("Só notas rejeitadas podem ser reenviadas");
        }
        n.setStatus("processando");
        n.setTentativasEnvio(0);
        n.setCodigoStatus(null);
        n.setMotivoRejeicao(null);
        n.setXmlPath(null);
        if (!repo.salvarSeStatus(n, "rejeitada")) {
            throw new ConflitoException("A nota foi alterada por outra operação. Atualize e tente de novo");
        }
        saldo.aplicar(n, SaldoDevolucao.Evento.REENVIADA); // a rejeição tinha liberado o "em devolução"
        mensagens.gerarXml(n);
        return NotaDto.de(n);
    }

    /** HU-B07.5: pede o cancelamento à SEFAZ; a nota fica "processando" até a resposta (EPB-10). */
    public NotaDto cancelar(long id, String justificativa, UsuarioAutenticado usuario) {
        String texto = justificativa == null ? "" : justificativa.trim();
        if (texto.length() < JUSTIFICATIVA_MINIMA) {
            throw invalido("justificativa", "A justificativa precisa ter pelo menos 15 caracteres");
        }
        NotaItem n = existente(id);
        if (n.getCancelamentoId() != null && "processando".equals(n.getStatus())) {
            throw new ConflitoException("Já existe um cancelamento em processamento para esta nota");
        }
        if ("ENTRADA".equals(n.getTipo())) {
            throw new RegraNegocioException("Nota de entrada é do fornecedor e não é cancelada por aqui");
        }
        if (!"autorizada".equals(n.getStatus())) {
            throw new RegraNegocioException("Só notas autorizadas podem ser canceladas");
        }
        Instant autorizada = Instant.parse(n.getAutorizadaEm() != null ? n.getAutorizadaEm() : n.getEmitidaEm());
        if (autorizada.plus(props.getPrazoCancelamento()).isBefore(clock.instant())) {
            throw new RegraNegocioException("Prazo de cancelamento expirado");
        }

        EventoFiscal e = new EventoFiscal();
        e.setId(ids.proximo("eventos_fiscais"));
        e.setTipo(EventoFiscal.CANCELAMENTO);
        e.setStatus("processando");
        e.setNotaId(n.getId());
        e.setModelo(n.getModelo());
        e.setSerie(n.getSerie());
        e.setJustificativa(texto);
        e.setUsuarioId(usuario.id());
        e.setRegistradoEm(clock.instant().toString());

        n.setStatus("processando");
        n.setCancelamentoId(e.getId());
        try {
            dynamo.transactWriteItems(r -> r.transactItems(repo.seStatusTx(n, "autorizada"), eventos.criarTx(e)));
        } catch (TransactionCanceledException ex) {
            throw new ConflitoException("Já existe um cancelamento em processamento para esta nota");
        }
        mensagens.eventoFiscal(e, n);
        return NotaDto.de(n);
    }

    static ValidacaoException invalido(String campo, String msg) {
        return new ValidacaoException(msg, List.of(new CampoErro(campo, msg)));
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    static String inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(FUSO_LOJA).toInstant().toString();
    }

    private static LocalDate data(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw invalido(campo, "Data inválida (use AAAA-MM-DD)");
        }
    }
}
