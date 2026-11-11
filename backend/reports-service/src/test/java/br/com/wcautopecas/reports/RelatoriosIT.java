package br.com.wcautopecas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.reports.fontes.Fontes;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

/** HU-B14.1 a 14.3: dashboard e relatórios de vendas e estoque pela API. */
class RelatoriosIT extends BaseIT {

    @Autowired
    StringRedisTemplate redis;

    static Fontes.Venda venda(long id, Long clienteId, Long vendedorId, String total) {
        return new Fontes.Venda(id, String.format("%06d", id), "2026-09-15T13:30:00Z", clienteId, vendedorId,
                "nfe", "pix", new BigDecimal(total), BigDecimal.ZERO, new BigDecimal(total), "finalizada", 40L + id);
    }

    @BeforeEach
    void preparar() {
        token = login(2, Papel.VENDEDOR);
        redis.delete("dashboard:stats");
        given(fontes.vendas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).willReturn(List.of(
                venda(1, 3L, 2L, "1234.50"), venda(2, null, 2L, "89.90"), venda(3, 3L, 5L, "10.00")));
        given(fontes.nomesClientes()).willReturn(Map.of(3L, "Oficina São João; Ltda"));
        given(fontes.estoque()).willReturn(List.of(
                new Fontes.Posicao(1, "PF-001", "Pastilha", "87083010", 45, 2, 43, 15, "ok", new BigDecimal("45.50"),
                        new BigDecimal("89.90")),
                new Fontes.Posicao(2, "PF-002", "Pastilha traseira", "87083010", 3, 0, 3, 15, "critico",
                        new BigDecimal("39.90"), new BigDecimal("79.90"))));
        given(fontes.resumoEstoque()).willReturn(new Fontes.ResumoEstoque(2, 1, new BigDecimal("2167.20"), 1));
        given(fontes.resumoClientes(any())).willReturn(new Fontes.ResumoClientes(10, 9, 1));
        given(fiscal.listar(isNull(), any(), any())).willReturn(List.of());
    }

    @Test
    void relatorioDeVendasEmCsv() {
        var r = get("/api/relatorios/vendas?de=2026-09-01&ate=2026-09-30&formato=csv", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("vendas_2026-09-01_2026-09-30.csv");
        List<String> linhas = new String(r.getBody(), StandardCharsets.UTF_8).lines().toList();
        assertThat(linhas).hasSize(4);
        assertThat(linhas.get(0)).isEqualTo("﻿Numero;Data;Cliente;Tipo_Documento;Metodo_Pagamento;Subtotal;"
                + "Desconto;Total;Nota_Fiscal;Status");
        assertThat(linhas.get(1)).isEqualTo(
                "000001;15/09/2026 10:30;\"Oficina São João; Ltda\";NFE;pix;1234,50;0,00;1234,50;41;finalizada");
        assertThat(linhas.get(2)).contains(";Consumidor final;");
    }

    @Test
    void relatorioDeVendasEmXlsxComFiltros() throws Exception {
        var r = get("/api/relatorios/vendas?de=2026-09-01&ate=2026-09-30&formato=xlsx&clienteId=3&vendedorId=2",
                byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("vendas_2026-09-01_2026-09-30.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(r.getBody()))) {
            var aba = wb.getSheetAt(0);
            assertThat(aba.getLastRowNum()).isEqualTo(1);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("000001");
            assertThat(aba.getRow(1).getCell(7).getNumericCellValue()).isEqualTo(1234.5);
        }
    }

    @Test
    void periodoDoRelatorioDeVendas() {
        assertThat(get("/api/relatorios/vendas?ate=2026-09-30", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/api/relatorios/vendas?de=2026-09-30&ate=2026-09-01", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        var longo = get("/api/relatorios/vendas?de=2025-01-01&ate=2026-09-01", JsonNode.class);
        assertThat(longo.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(longo.getBody().get("message").asText()).contains("366 dias");
        assertThat(get("/api/relatorios/vendas?de=2026-09-01&ate=2026-09-30&formato=pdf", JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void relatorioDeEstoqueFiltradoPorStatus() throws Exception {
        var r = get("/api/relatorios/estoque?formato=xlsx&status=critico", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentDisposition().getFilename()).startsWith("estoque_critico_").endsWith(".xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(r.getBody()))) {
            var aba = wb.getSheetAt(0);
            assertThat(aba.getRow(0).getCell(10).getStringCellValue()).isEqualTo("Valor_Estoque");
            assertThat(aba.getLastRowNum()).isEqualTo(1);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("PF-002");
            assertThat(aba.getRow(1).getCell(10).getNumericCellValue()).isEqualTo(119.7);
        }
        String csv = new String(get("/api/relatorios/estoque", byte[].class).getBody(), StandardCharsets.UTF_8);
        assertThat(csv.lines()).hasSize(3);
        assertThat(csv).contains("PF-001;Pastilha;87083010;45;2;43;15;ok;45,50;89,90;2047,50");
        assertThat(get("/api/relatorios/estoque?status=zerado", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void dashboardComCacheERecalculoSoDoAdmin() {
        given(fontes.vendas(any(), any())).willReturn(List.of(new Fontes.Venda(9, "000009",
                Instant.now().toString(), null, 2L, "nfce", "pix", new BigDecimal("200"), BigDecimal.ZERO,
                new BigDecimal("200"), "finalizada", null)));

        var r = get("/api/dashboard", JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode d = r.getBody();
        assertThat(d.fieldNames()).toIterable().containsExactly("vendas", "estoque", "fiscal", "clientes");
        assertThat(d.get("vendas").fieldNames()).toIterable()
                .containsExactly("hoje", "valorHoje", "mes", "valorMes", "ticketMedio");
        assertThat(d.get("estoque").fieldNames()).toIterable()
                .containsExactly("totalProdutos", "estoqueBaixo", "itensCriticos", "valorEmEstoque", "criticos");
        assertThat(d.get("fiscal").fieldNames()).toIterable()
                .containsExactly("emitidasHoje", "autorizadas", "processando", "rejeitadas", "valorHoje");
        assertThat(d.get("clientes").fieldNames()).toIterable().containsExactly("total", "ativos", "novosNoMes");
        assertThat(d.get("vendas").get("hoje").asLong()).isEqualTo(1);
        assertThat(d.get("vendas").get("valorHoje").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(d.get("estoque").get("criticos").get(0).get("codigo").asText()).isEqualTo("PF-002");
        assertThat(d.get("clientes").get("novosNoMes").asLong()).isEqualTo(1);

        // Segunda leitura vem do Redis: os serviços não são chamados de novo.
        get("/api/dashboard", JsonNode.class);
        verify(fontes, times(1)).resumoEstoque();

        assertThat(get("/api/dashboard?atualizar=true", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        token = login(1, Papel.ADMIN);
        assertThat(get("/api/dashboard?atualizar=true", JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fontes, times(2)).resumoEstoque();
    }
}
