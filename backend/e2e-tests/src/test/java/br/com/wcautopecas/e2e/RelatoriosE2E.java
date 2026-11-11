package br.com.wcautopecas.e2e;

import static br.com.wcautopecas.e2e.Stack.PRAZO;
import static br.com.wcautopecas.e2e.Stack.aguardarNotaDaVenda;
import static br.com.wcautopecas.e2e.Stack.api;
import static br.com.wcautopecas.e2e.Stack.garantirEstoque;
import static br.com.wcautopecas.e2e.Stack.json;
import static br.com.wcautopecas.e2e.Stack.valorSemGatilho;
import static br.com.wcautopecas.e2e.Stack.venda;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Extras do EPB-14 pelo gateway: dashboard e relatórios de vendas e estoque com dados reais da stack. */
@DisplayName("EPB-14 Dashboard e relatórios")
class RelatoriosE2E {

    private static final long PF_001 = 1;

    @Test
    @DisplayName("Venda de hoje aparece no dashboard e no relatório de vendas")
    void vendaNoDashboardENoRelatorio() {
        garantirEstoque(PF_001, 5);
        long vendaId = venda("nfce", null, PF_001, 1, valorSemGatilho(PF_001, 1));
        aguardarNotaDaVenda(vendaId, "autorizada", PRAZO);

        JsonNode d = json(api().queryParam("atualizar", true).get("/dashboard").then().statusCode(200).extract()
                .asString());
        assertThat(d.path("vendas").path("hoje").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.path("fiscal").path("emitidasHoje").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.path("estoque").path("totalProdutos").asLong()).isPositive();
        assertThat(d.path("clientes").path("total").asLong()).isPositive();

        String hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();
        byte[] csv = api().queryParam("de", hoje).queryParam("ate", hoje).get("/relatorios/vendas").then()
                .statusCode(200).header("Content-Disposition", "attachment; filename=\"vendas_" + hoje + "_" + hoje
                        + ".csv\"").extract().asByteArray();
        List<String> linhas = new String(csv, StandardCharsets.UTF_8).lines().toList();
        assertThat(linhas.get(0)).endsWith("Numero;Data;Cliente;Tipo_Documento;Metodo_Pagamento;Subtotal;Desconto;"
                + "Total;Nota_Fiscal;Status");
        assertThat(linhas).anyMatch(l -> l.startsWith(String.format("VD-%06d;", vendaId))
                && l.contains(";Consumidor final;NFCE;pix;") && l.endsWith(";finalizada"));
    }

    @Test
    @DisplayName("Relatório de estoque filtrado por status e em XLSX")
    void relatorioDeEstoque() {
        String csv = new String(api().queryParam("status", "critico").get("/relatorios/estoque").then()
                .statusCode(200).extract().asByteArray(), StandardCharsets.UTF_8);
        List<String> linhas = csv.lines().toList();
        assertThat(linhas.get(0)).endsWith("Codigo;Nome;NCM;Estoque_Atual;Reservado;Disponivel;Estoque_Minimo;Status;"
                + "Preco_Custo;Preco_Venda;Valor_Estoque");
        assertThat(linhas.subList(1, linhas.size())).allMatch(l -> l.contains(";critico;"));

        byte[] xlsx = api().queryParam("formato", "xlsx").get("/relatorios/estoque").then().statusCode(200)
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").extract()
                .asByteArray();
        assertThat(new String(xlsx, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
    }
}
