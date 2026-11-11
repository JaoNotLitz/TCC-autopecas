package br.com.wcautopecas.reports.planilha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.wcautopecas.commons.erro.ValidacaoException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** TC-B14.2.1 e TC-B14.2.2: o gerador de CSV e XLSX. */
class PlanilhaTest {

    Planilha exemplo() {
        return new Planilha("Vendas", List.of("Numero", "Cliente", "Total", "Nota_Fiscal"))
                .linha("000001", "Oficina São João; filial \"Centro\"", new BigDecimal("1234.5"), 7L)
                .linha("000002", null, new BigDecimal("89.9"), null);
    }

    @Test
    void csvComBomPontoEVirgulaEDecimalComVirgula() {
        Planilha.Arquivo a = exemplo().gerar(Planilha.Formato.CSV, "vendas_2026-09-01_2026-09-30");
        assertThat(a.nome()).isEqualTo("vendas_2026-09-01_2026-09-30.csv");
        assertThat(a.linhas()).isEqualTo(2);
        String csv = new String(a.conteudo(), StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("﻿Numero;Cliente;Total;Nota_Fiscal\r\n"
                + "000001;\"Oficina São João; filial \"\"Centro\"\"\";1234,50;7\r\n"
                + "000002;;89,90;\r\n");
    }

    @Test
    void xlsxAbreNoPoiComCabecalhoEmNegritoENumeros() throws Exception {
        Planilha.Arquivo a = exemplo().gerar(Planilha.Formato.XLSX, "vendas");
        assertThat(a.nome()).isEqualTo("vendas.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(a.conteudo()))) {
            Sheet aba = wb.getSheet("Vendas");
            assertThat(aba.getLastRowNum()).isEqualTo(2);
            assertThat(aba.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Cliente");
            assertThat(wb.getFontAt(aba.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(aba.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Oficina São João; filial \"Centro\"");
            assertThat(aba.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(1234.5);
            assertThat(aba.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(7);
        }
    }

    @Test
    void formato() {
        assertThat(Planilha.Formato.de(null)).isEqualTo(Planilha.Formato.CSV);
        assertThat(Planilha.Formato.de("XLSX")).isEqualTo(Planilha.Formato.XLSX);
        assertThatThrownBy(() -> Planilha.Formato.de("pdf")).isInstanceOf(ValidacaoException.class);
    }
}
