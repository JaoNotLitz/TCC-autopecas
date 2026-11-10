package br.com.wcautopecas.reports.fiscal;

import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** HU-B14.4: CSV das notas do Painel Fiscal, com os mesmos filtros de {@code GET /api/notas}. */
@Service
public class RelatorioNotasService {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final Set<String> STATUS = Set.of("processando", "autorizada", "cancelada", "rejeitada");
    static final String CABECALHO = "Tipo,Numero,Serie,Chave,Cliente,Documento,Valor,Emitida_Em,Status";
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** BOM: o Excel só lê os acentos de um CSV UTF-8 com ele. */
    static final String BOM = "﻿";

    private final NotasFiscais notas;
    private final Clock clock;

    public RelatorioNotasService(NotasFiscais notas, Clock clock) {
        this.notas = notas;
        this.clock = clock;
    }

    public record Relatorio(byte[] csv, String nomeArquivo, int linhas) {
    }

    public Relatorio gerar(String status, String de, String ate) {
        String filtroStatus = vazio(status) ? null : status;
        if (filtroStatus != null && !STATUS.contains(filtroStatus)) {
            throw invalido("status", "Status inválido");
        }
        YearMonth mes = YearMonth.now(clock.withZone(FUSO));
        LocalDate inicio = vazio(de) ? mes.atDay(1) : data(de, "de");
        LocalDate fim = vazio(ate) ? mes.atEndOfMonth() : data(ate, "ate");
        if (fim.isBefore(inicio)) {
            throw invalido("ate", "A data final não pode ser anterior à inicial");
        }
        List<NotaResumo> lista = notas.listar(filtroStatus, inicio, fim);
        StringBuilder csv = new StringBuilder(BOM).append(CABECALHO).append("\r\n");
        for (NotaResumo n : lista) {
            csv.append(String.join(",", campo(n.tipo()), campo(n.numero()), campo(n.serie()),
                    campo(n.chaveAcesso()), campo(n.clienteNome()), campo(n.clienteDocumento()),
                    valor(n.valorTotal()), campo(dataHora(n.emitidaEm())), campo(n.status()))).append("\r\n");
        }
        return new Relatorio(csv.toString().getBytes(StandardCharsets.UTF_8),
                "notas_" + inicio + "_" + fim + ".csv", lista.size());
    }

    /** RFC 4180: aspas quando o texto tem vírgula, aspas ou quebra de linha. */
    static String campo(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static String valor(BigDecimal v) {
        return v == null ? "" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String dataHora(String iso) {
        if (iso == null || iso.isBlank()) {
            return "";
        }
        try {
            return DATA_HORA.format(Instant.parse(iso).atZone(FUSO));
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static LocalDate data(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw invalido(campo, "Data inválida (use AAAA-MM-DD)");
        }
    }

    private static ValidacaoException invalido(String campo, String msg) {
        return new ValidacaoException(msg, List.of(new CampoErro(campo, msg)));
    }
}
