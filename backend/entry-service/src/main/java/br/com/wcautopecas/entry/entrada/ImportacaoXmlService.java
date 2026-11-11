package br.com.wcautopecas.entry.entrada;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.commons.validacao.Documentos;
import br.com.wcautopecas.entry.EntradasProperties;
import br.com.wcautopecas.entry.api.EntradaInput;
import br.com.wcautopecas.entry.api.NotaFiscalDto;
import br.com.wcautopecas.entry.integracao.Integracoes;
import br.com.wcautopecas.entry.xml.NfeEntradaParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Importação assíncrona do XML do fornecedor (HU-B11.3 e EPB-12, extensão sem tela no front).
 * O upload guarda o XML no {@code wc-xml-storage}, cria a entrada {@code recebida} e publica
 * {@code entry.uploaded}; o {@link XmlProcessorListener} lê, valida com as regras da entrada manual,
 * vincula os itens pelo código e registra. Itens sem produto no catálogo ficam {@code vinculado = false}
 * e não movimentam estoque (decisão A5).
 */
@Service
public class ImportacaoXmlService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoXmlService.class);
    static final String EVENTO = "entry.uploaded";

    public enum Resultado { REGISTRADA, COM_PENDENCIAS, ERRO, IGNORADA }

    /** Resposta 202 do upload (RF03 da HU-B11.3). */
    public record LoteAceito(long uploadId, String s3Key, String message) {
    }

    private final EntradaRepository repo;
    private final EntradaService entradas;
    private final Integracoes integracoes;
    private final IdGenerator ids;
    private final Outbox outbox;
    private final EventPublisher publisher;
    private final IdempotencyGuard locks;
    private final S3Client s3;
    private final Validator validator;
    private final EntradasProperties props;
    private final Clock clock;

    public ImportacaoXmlService(EntradaRepository repo, EntradaService entradas, Integracoes integracoes,
            IdGenerator ids, Outbox outbox, EventPublisher publisher, IdempotencyGuard locks, S3Client s3,
            Validator validator, EntradasProperties props, Clock clock) {
        this.repo = repo;
        this.entradas = entradas;
        this.integracoes = integracoes;
        this.ids = ids;
        this.outbox = outbox;
        this.publisher = publisher;
        this.locks = locks;
        this.s3 = s3;
        this.validator = validator;
        this.props = props;
        this.clock = clock;
    }

    // ---- HU-B11.3: upload

    /**
     * Aceita o arquivo se parecer XML. A leitura completa fica para o listener: um XML malformado vira
     * entrada {@code erro} com o motivo e vai para a DLQ (HU-B12.3), em vez de sumir num 400.
     */
    public LoteAceito aceitar(byte[] xml, UsuarioAutenticado usuario) {
        if (!pareceXml(xml)) {
            throw new ValidacaoException("O arquivo não é um XML");
        }
        EntradaInput lido = null;
        try {
            lido = NfeEntradaParser.ler(xml, entradas.hoje());
        } catch (RuntimeException e) {
            log.info("XML recebido não pôde ser lido agora ({}); o processamento registra o erro", e.getMessage());
        }
        String chave = lido == null ? null : Documentos.digitos(lido.chaveAcesso());
        if (chave != null && chave.length() != 44) {
            chave = null;
        }
        if (chave != null && repo.porChave(chave).isPresent()) {
            throw new ConflitoException("Nota de entrada já registrada com esta chave de acesso");
        }
        String cnpj = lido == null || lido.fornecedor() == null ? null : Documentos.digitos(lido.fornecedor().cnpj());
        LocalDate data = lido == null || lido.dataEmissao() == null ? entradas.hoje() : lido.dataEmissao();

        long id = ids.proximo("entradas");
        String key = String.format("fornecedor/%s/%04d/%02d/%s.xml", cnpj == null || cnpj.isBlank()
                ? "desconhecido" : cnpj, data.getYear(), data.getMonthValue(), chave == null ? "entrada-" + id : chave);
        s3.putObject(r -> r.bucket(props.getBucketXml()).key(key).contentType("application/xml"),
                RequestBody.fromBytes(xml));

        NotaEntrada e = new NotaEntrada();
        e.setId(id);
        e.setStatus("recebida");
        e.setOrigem("xml");
        e.setS3Key(key);
        e.setChaveAcesso(chave);
        e.setFornecedorCnpj(cnpj);
        e.setUsuarioId(usuario.id());
        e.setRegistradaEm(clock.instant().toString());
        repo.criar(e);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventType", EVENTO);
        evento.put("entryId", id);
        evento.put("s3Key", key);
        evento.put("timestamp", clock.instant().toString());
        outbox.fila(props.getFilaEntradas(), EVENTO, evento);
        log.info("XML da entrada {} recebido em {}", id, key);
        return new LoteAceito(id, key, "XML recebido. A entrada será registrada em instantes");
    }

    private static boolean pareceXml(byte[] xml) {
        String inicio = new String(xml, 0, Math.min(xml.length, 64), StandardCharsets.UTF_8).replace("﻿", "")
                .stripLeading();
        return inicio.startsWith("<");
    }

    // ---- EPB-12: processamento

    /**
     * Processa a entrada. Reentrega de uma entrada já registrada não faz nada (HU-B12.3 RF02); falha de
     * rede ou do product/fiscal-service relança para a SQS tentar de novo.
     *
     * @param redrive mensagem devolvida pelo {@code wc-dlq-redrive}: uma entrada em erro é processada de novo
     */
    public Resultado processar(long entradaId, String s3Key, boolean redrive) {
        NotaEntrada e = repo.porId(entradaId).orElse(null);
        if (e == null) {
            log.warn("entrada {} não existe; mensagem descartada", entradaId);
            return Resultado.IGNORADA;
        }
        if ("registrada".equals(e.getStatus()) || "com_pendencias".equals(e.getStatus())
                || ("erro".equals(e.getStatus()) && !redrive)) {
            log.info("entrada {} já está {}; reentrega ignorada", entradaId, e.getStatus());
            return Resultado.IGNORADA;
        }
        String key = s3Key == null || s3Key.isBlank() ? e.getS3Key() : s3Key;
        byte[] xml = s3.getObjectAsBytes(r -> r.bucket(props.getBucketXml()).key(key)).asByteArray();

        EntradaInput in;
        try {
            in = NfeEntradaParser.ler(xml, entradas.hoje());
        } catch (RuntimeException ex) {
            return paraDlq(e, key, ex instanceof NfeEntradaParser.XmlInvalidoException ? ex.getMessage()
                    : "XML inválido ou não é NF-e");
        }
        String invalido = validar(in);
        if (invalido != null) {
            return paraDlq(e, key, invalido);
        }

        String chave = Documentos.digitos(in.chaveAcesso());
        if (outraComChave(chave, entradaId).isPresent()) {
            return erro(e, "Chave já registrada por outra entrada");
        }
        String lock = "entrada:" + chave;
        if (!locks.primeiraVez(lock, Duration.ofSeconds(30))) {
            throw new IllegalStateException("chave " + chave + " sendo registrada agora; tenta de novo depois");
        }
        try {
            Optional<NotaEntrada> outra = outraComChave(chave, entradaId);
            if (outra.isPresent()) {
                return erro(e, "Chave já registrada pela entrada " + outra.get().getId());
            }
            return registrar(e, in, chave);
        } finally {
            locks.liberar(lock);
        }
    }

    private Resultado registrar(NotaEntrada e, EntradaInput in, String chave) {
        String cnpj = Documentos.digitos(in.fornecedor().cnpj());
        List<String> codigos = in.itens().stream().map(i -> i.codigo() == null ? "" : i.codigo().trim())
                .filter(c -> !c.isEmpty()).distinct().toList();
        Map<String, Long> catalogo = new LinkedHashMap<>();
        integracoes.produtosPorCodigo(codigos).forEach(c -> {
            if (c.produtoId() != null) {
                catalogo.putIfAbsent(c.codigo().trim(), c.produtoId());
            }
        });

        List<ItemEntrada> itens = new ArrayList<>();
        List<Integracoes.ItemNota> vinculados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (EntradaInput.Item it : in.itens()) {
            Long produto = it.codigo() == null ? null : catalogo.get(it.codigo().trim());
            ItemEntrada i = new ItemEntrada();
            i.setProdutoId(produto);
            i.setVinculado(produto != null);
            i.setCodigo(it.codigo());
            i.setDescricao(it.descricao());
            i.setNcm(it.ncm());
            i.setCfop(it.cfop());
            i.setQuantidade(it.quantidade());
            i.setValorUnitario(it.valorUnitario());
            itens.add(i);
            if (produto != null) {
                vinculados.add(new Integracoes.ItemNota(produto, it.codigo(), it.descricao(), it.ncm(), it.cfop(),
                        it.quantidade(), it.valorUnitario()));
            }
            total = total.add(it.valorUnitario().multiply(BigDecimal.valueOf(it.quantidade())));
        }

        Fornecedor f = entradas.fornecedor(in.fornecedor(), cnpj);
        // A nota ENTRADA do Painel Fiscal leva só os itens com produto; sem nenhum, fica sem nota até o vínculo.
        // O fiscal-service é idempotente pela chave, então a reentrega depois de uma queda devolve a mesma nota.
        if (!vinculados.isEmpty()) {
            NotaFiscalDto nota = integracoes.registrarNota(new Integracoes.NotaEntradaFiscal(e.getId(), f.getId(),
                    in.numero(), in.serie(), chave, in.dataEmissao(), vinculados));
            e.setNotaFiscalId(nota.id());
        }

        boolean pendente = vinculados.size() < itens.size();
        e.setStatus(pendente ? "com_pendencias" : "registrada");
        e.setErro(null);
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
        e.setItens(itens);
        e.setRegistradaEm(clock.instant().toString());
        repo.salvar(e);

        entradas.somarEstoque(e.getId(), itens, in.numero(), f.getRazaoSocial(), e.getUsuarioId());
        log.info("entrada {} importada do XML: {}, {} de {} itens vinculados", e.getId(), e.getStatus(),
                vinculados.size(), itens.size());
        return pendente ? Resultado.COM_PENDENCIAS : Resultado.REGISTRADA;
    }

    /** Mesmas regras da entrada manual: anotações do {@code EntradaInput} e datas. */
    private String validar(EntradaInput in) {
        List<String> erros = validator.validate(in).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage).distinct().collect(Collectors.toCollection(ArrayList::new));
        if (in.dataEmissao() != null && in.dataEmissao().isAfter(entradas.hoje())) {
            erros.add("A data de emissão não pode estar no futuro");
        }
        return erros.isEmpty() ? null : String.join("; ", erros);
    }

    private Optional<NotaEntrada> outraComChave(String chave, long entradaId) {
        return repo.porChave(chave).filter(o -> o.getId() != entradaId);
    }

    private Resultado erro(NotaEntrada e, String motivo) {
        e.setStatus("erro");
        e.setErro(motivo);
        repo.salvar(e);
        log.warn("entrada {} com erro: {}", e.getId(), motivo);
        return Resultado.ERRO;
    }

    /**
     * XML que nunca vai passar (malformado, não-NF-e, chave ou dados inválidos): entrada {@code erro} e
     * mensagem direto na DLQ (seção 8.6), com a origem para o redrive devolver depois de corrigir.
     */
    private Resultado paraDlq(NotaEntrada e, String key, String motivo) {
        erro(e, motivo);
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventType", EVENTO);
        evento.put("entryId", e.getId());
        evento.put("s3Key", key);
        evento.put("erro", motivo);
        evento.put("timestamp", clock.instant().toString());
        publisher.enviarFila(props.getFilaDlq(), EVENTO, evento,
                Map.of(EventPublisher.ORIGEM_FILA, props.getFilaEntradas()));
        return Resultado.ERRO;
    }
}
