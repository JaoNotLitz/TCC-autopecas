package br.com.wcautopecas.notification.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.notification.NotificacoesProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailUnitTest {

    final ObjectMapper mapper = new ObjectMapper();
    final Templates templates = new Templates(new Templates.Config().emailTemplateEngine());
    final NotificacoesProperties props = new NotificacoesProperties();
    final PedidosEmail pedidos = new PedidosEmail(mapper, props);

    {
        props.setAlertaEstoqueDestinatarios(List.of("compras@wcautopecas.local"));
        props.setOpsDestinatarios(List.of("ops@wcautopecas.local", "ti@wcautopecas.local"));
    }

    static final Map<String, Map<String, Object>> EXEMPLOS = Map.of(
            "welcome", Map.of("nome", "João da Silva", "atualizacao", false),
            "sale-confirmation", Map.of("clienteNome", "João", "numero", "000042", "vendaId", 42,
                    "valorTotal", 1234.5, "notaFiscalId", 7),
            "low-stock-alert", Map.of("codigo", "PF-001", "nome", "Pastilha de freio", "estoqueAtual", 2,
                    "estoqueMinimo", 10),
            "fiscal-error", Map.of("service", "fiscal-service", "severity", "critical",
                    "message", "Nota 9: rejeição 539", "timestamp", "2026-11-10T22:00:00Z"),
            "contador-lote", Map.of("mes", "11/2026", "quantidade", 25, "link", "http://x/lote.zip",
                    "validadeDias", 7, "parte", 1, "partes", 2));

    @Test
    void todosOsTemplatesRenderizam() {
        assertThat(Templates.ASSUNTOS.keySet()).isEqualTo(EXEMPLOS.keySet());
        EXEMPLOS.forEach((t, dados) -> {
            String html = templates.renderizar(t, dados);
            assertThat(html).as(t).startsWith("<!DOCTYPE html>").contains("WC Autopeças").doesNotContain(" th:").doesNotContain("xmlns:th");
        });
        assertThat(templates.renderizar("welcome", EXEMPLOS.get("welcome"))).contains("João da Silva")
                .contains("Seu cadastro").doesNotContain("Este é agora");
        assertThat(templates.renderizar("welcome", Map.of("nome", "Ana", "atualizacao", true))).contains("Este é agora");
        assertThat(templates.renderizar("sale-confirmation", EXEMPLOS.get("sale-confirmation")))
                .contains("R$ 1.234,50").contains("000042");
        assertThat(templates.renderizar("contador-lote", EXEMPLOS.get("contador-lote")))
                .contains("href=\"http://x/lote.zip\"").contains("parte <span>1</span> de <span>2</span>");
        assertThat(templates.renderizar("low-stock-alert", EXEMPLOS.get("low-stock-alert"))).contains("PF-001");
    }

    @Test
    void pedidoProntoDaOutbox() throws Exception {
        var p = pedidos.de(mapper.readTree("""
                {"to":"a@x.com, b@x.com","cc":["c@x.com"],"subject":"Oi","template":"contador-lote",
                 "data":{"mes":"11/2026"}}""")).orElseThrow();
        assertThat(p.to()).containsExactly("a@x.com", "b@x.com");
        assertThat(p.cc()).containsExactly("c@x.com");
        assertThat(p.template()).isEqualTo("contador-lote");
        assertThat(p.data()).containsEntry("mes", "11/2026");
    }

    @Test
    void alertasDosTopicos() throws Exception {
        var estoque = pedidos.de(mapper.readTree("""
                {"MessageType":"low_stock_alert","codigo":"PF-001","nome":"Pastilha","estoqueAtual":1,
                 "estoqueMinimo":10,"status":"critico"}""")).orElseThrow();
        assertThat(estoque.template()).isEqualTo("low-stock-alert");
        assertThat(estoque.to()).containsExactly("compras@wcautopecas.local");
        assertThat(estoque.subject()).isEqualTo("Estoque crítico: Pastilha");

        var venda = pedidos.de(mapper.readTree("""
                {"MessageType":"sale_completed","numero":"000042","clienteEmail":"joao@email.com",
                 "clienteNome":"João","valorTotal":800}""")).orElseThrow();
        assertThat(venda.template()).isEqualTo("sale-confirmation");
        assertThat(venda.to()).containsExactly("joao@email.com");
        assertThat(venda.subject()).contains("000042");

        var sistema = pedidos.de(mapper.readTree("""
                {"MessageType":"fiscal-error","service":"fiscal-service","severity":"critical","message":"x"}"""))
                .orElseThrow();
        assertThat(sistema.template()).isEqualTo("fiscal-error");
        assertThat(sistema.to()).hasSize(2);
        assertThat(sistema.subject()).isEqualTo("[CRITICAL] Alerta do fiscal-service");

        assertThat(pedidos.de(mapper.readTree("{\"MessageType\":\"warning\",\"severity\":\"low\"}"))).isEmpty();
        assertThat(pedidos.de(mapper.readTree("{\"MessageType\":\"desconhecido\"}"))).isEmpty();
    }

    @Test
    void envelopeSns() throws Exception {
        String interno = "{\"MessageType\":\"sale_completed\",\"clienteEmail\":\"ana@email.com\",\"valorTotal\":10}";
        var p = pedidos.de(mapper.createObjectNode().put("Type", "Notification").put("Message", interno))
                .orElseThrow();
        assertThat(p.to()).containsExactly("ana@email.com");
    }

    @Test
    void destinoInvalidoNaoEnvia() {
        EmailSender sender = mock(EmailSender.class);
        EnvioEmail envio = new EnvioEmail(templates, sender);
        assertThat(envio.enviar(new PedidoEmail(List.of("joao@"), List.of(), null, "welcome",
                Map.of("nome", "João"), null))).isEmpty();
        assertThat(envio.enviar(new PedidoEmail(List.of("joao@email.com"), List.of(), null, "nao-existe",
                Map.of(), null))).isEmpty();
        verify(sender, never()).enviar(any(), anyString(), anyString());
    }

    @Test
    void ccInvalidoEhIgnoradoEAssuntoPadrao() {
        EmailSender sender = mock(EmailSender.class);
        given(sender.enviar(any(), anyString(), anyString())).willReturn("id-1");
        EnvioEmail envio = new EnvioEmail(templates, sender);
        assertThat(envio.enviar(new PedidoEmail(List.of("joao@email.com", "joao@"), List.of("x"), null, "welcome",
                Map.of("nome", "João"), null))).contains("id-1");
        ArgumentCaptor<PedidoEmail> p = ArgumentCaptor.forClass(PedidoEmail.class);
        verify(sender).enviar(p.capture(), eq("Bem-vindo(a) à WC Autopeças"), anyString());
        assertThat(p.getValue().to()).containsExactly("joao@email.com");
        assertThat(p.getValue().cc()).isEmpty();
    }

    @Test
    void validacaoDeEmail() {
        assertThat(EnvioEmail.valido("joao@email.com")).isTrue();
        assertThat(EnvioEmail.valido("joao.silva+loja@wc.com.br")).isTrue();
        assertThat(EnvioEmail.valido("joao@")).isFalse();
        assertThat(EnvioEmail.valido("joao@email")).isFalse();
        assertThat(EnvioEmail.valido("a b@email.com")).isFalse();
        assertThat(EnvioEmail.valido(null)).isFalse();
    }
}
