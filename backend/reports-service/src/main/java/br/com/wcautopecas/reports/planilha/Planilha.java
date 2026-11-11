package br.com.wcautopecas.reports.planilha;

import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;

/**
 * Gerador genérico de CSV e XLSX dos relatórios (ST-B14.2.1). O CSV sai do jeito que o Excel pt-BR abre
 * sem assistente: UTF-8 com BOM, separador {@code ;} e decimal com vírgula. O XLSX tem o cabeçalho em
 * negrito e números como números.
 */
public final class Planilha {

    public enum Formato {
        CSV("csv", new MediaType("text", "csv", StandardCharsets.UTF_8)),
        XLSX("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        public final String extensao;
        public final MediaType tipo;

        Formato(String extensao, MediaType tipo) {
            this.extensao = extensao;
            this.tipo = tipo;
        }

        /** {@code csv} (padrão) ou {@code xlsx}; outro valor responde 400. */
        public static Formato de(String valor) {
            if (valor == null || valor.isBlank() || "csv".equalsIgnoreCase(valor.trim())) {
                return CSV;
            }
            if ("xlsx".equalsIgnoreCase(valor.trim())) {
                return XLSX;
            }
            String msg = "Formato inválido (use csv ou xlsx)";
            throw new ValidacaoException(msg, List.of(new CampoErro("formato", msg)));
        }
    }

    /** Arquivo pronto para o download. */
    public record Arquivo(byte[] conteudo, String nome, MediaType tipo, int linhas) {
    }

    static final String BOM = "﻿";

    private final String nomeAba;
    private final List<String> colunas;
    private final List<List<Object>> linhas = new ArrayList<>();

    public Planilha(String nomeAba, List<String> colunas) {
        this.nomeAba = nomeAba;
        this.colunas = List.copyOf(colunas);
    }

    /** Valores: {@code String}, {@code BigDecimal} (2 casas), {@code Number} inteiro ou {@code null}. */
    public Planilha linha(Object... valores) {
        if (valores.length != colunas.size()) {
            throw new IllegalArgumentException("esperava " + colunas.size() + " valores, veio " + valores.length);
        }
        linhas.add(java.util.Arrays.asList(valores));
        return this;
    }

    public Arquivo gerar(Formato formato, String nomeBase) {
        byte[] conteudo = formato == Formato.XLSX ? xlsx() : csv();
        return new Arquivo(conteudo, nomeBase + "." + formato.extensao, formato.tipo, linhas.size());
    }

    byte[] csv() {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(String.join(";", colunas.stream().map(Planilha::campoCsv).toList())).append("\r\n");
        for (List<Object> l : linhas) {
            sb.append(String.join(";", l.stream().map(Planilha::valorCsv).toList())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String valorCsv(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal d) {
            return d.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        }
        return campoCsv(v.toString());
    }

    /** Aspas quando o texto tem {@code ;}, aspas ou quebra de linha. */
    static String campoCsv(String s) {
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    byte[] xlsx() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet aba = wb.createSheet(nomeAba);
            Font negrito = wb.createFont();
            negrito.setBold(true);
            CellStyle cabecalho = wb.createCellStyle();
            cabecalho.setFont(negrito);
            CellStyle dinheiro = wb.createCellStyle();
            dinheiro.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            Row r0 = aba.createRow(0);
            for (int c = 0; c < colunas.size(); c++) {
                Cell cell = r0.createCell(c);
                cell.setCellValue(colunas.get(c));
                cell.setCellStyle(cabecalho);
            }
            for (int i = 0; i < linhas.size(); i++) {
                Row r = aba.createRow(i + 1);
                List<Object> l = linhas.get(i);
                for (int c = 0; c < l.size(); c++) {
                    Object v = l.get(c);
                    Cell cell = r.createCell(c);
                    if (v instanceof BigDecimal d) {
                        cell.setCellValue(d.setScale(2, RoundingMode.HALF_UP).doubleValue());
                        cell.setCellStyle(dinheiro);
                    } else if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (v != null) {
                        cell.setCellValue(v.toString());
                    }
                }
            }
            // Largura pelo texto mais longo: autoSizeColumn precisa de fontes do AWT, que a imagem não tem.
            for (int c = 0; c < colunas.size(); c++) {
                int largura = colunas.get(c).length();
                for (List<Object> l : linhas) {
                    largura = Math.max(largura, l.get(c) == null ? 0 : l.get(c).toString().length());
                }
                aba.setColumnWidth(c, Math.min(60, largura + 2) * 256);
            }
            aba.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
