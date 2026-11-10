package br.com.wcautopecas.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.teste.InfraTeste;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** E-mails conferidos no SES do LocalStack ({@code GET /_aws/ses}). */
@SpringBootTest(properties = {
        "wc.notificacoes.alerta-estoque-destinatarios=compras@wcautopecas.local",
        "wc.notificacoes.ops-destinatarios=ops@wcautopecas.local"})
@ActiveProfiles("test")
class NotificacaoIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
    }

    @Autowired
    EventPublisher publisher;
    @Autowired
    ObjectMapper mapper;

    static String unico(String nome) {
        return nome + "+" + UUID.randomUUID().toString().substring(0, 8) + "@email.com";
    }

    List<JsonNode> emailsPara(String destino) throws Exception {
        var resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(InfraTeste.endpoint() + "/_aws/ses")).build(),
                HttpResponse.BodyHandlers.ofString());
        List<JsonNode> r = new ArrayList<>();
        mapper.readTree(resp.body()).path("messages").forEach(m -> m.path("Destination").path("ToAddresses")
                .forEach(t -> {
                    if (destino.equals(t.asText())) {
                        r.add(m);
                    }
                }));
        return r;
    }

    JsonNode esperarEmail(String destino) {
        List<JsonNode> achados = new ArrayList<>();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            achados.clear();
            achados.addAll(emailsPara(destino));
            assertThat(achados).hasSize(1);
        });
        return achados.get(0);
    }

    static String html(JsonNode email) {
        return email.path("Body").path("html_part").asText();
    }

    @Test
    void pedidoDeBoasVindasViraEmail() {
        String destino = unico("joao");
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("to", destino);
        pedido.put("cc", List.of());
        pedido.put("subject", null);
        pedido.put("template", "welcome");
        pedido.put("data", Map.of("nome", "João Teste", "atualizacao", false));
        publisher.enviarFila("email-queue", "welcome", pedido);

        JsonNode email = esperarEmail(destino);
        assertThat(email.path("Source").asText()).isEqualTo("noreply@wcautopecas.com.br");
        assertThat(email.path("Subject").asText()).isEqualTo("Bem-vindo(a) à WC Autopeças");
        assertThat(html(email)).contains("João Teste");
    }

    @Test
    void destinoInvalidoNaoEnviaNada() throws Exception {
        publisher.enviarFila("email-queue", "welcome",
                Map.of("to", "joao@", "template", "welcome", "data", Map.of("nome", "x")));
        // um pedido válido logo atrás: quando ele chega, o inválido já foi consumido
        String depois = unico("depois");
        publisher.enviarFila("email-queue", "welcome",
                Map.of("to", depois, "template", "welcome", "data", Map.of("nome", "x")));
        esperarEmail(depois);
        assertThat(emailsPara("joao@")).isEmpty();
    }

    @Test
    void clienteCadastradoComEmailRecebeBoasVindas() throws Exception {
        String destino = unico("ana");
        publisher.enviarFila("customer-events-queue", "customer.created", Map.of("eventType", "customer.created",
                "customerId", 77, "data", Map.of("id", 77, "nome", "Ana Lima", "email", destino)));
        publisher.enviarFila("customer-events-queue", "customer.created", Map.of("eventType", "customer.created",
                "customerId", 78, "data", Map.of("id", 78, "nome", "Sem Email", "email", "")));
        JsonNode email = esperarEmail(destino);
        assertThat(html(email)).contains("Ana Lima");

        String novo = unico("ana-nova");
        publisher.enviarFila("customer-events-queue", "customer.updated", Map.of("eventType", "customer.updated",
                "customerId", 77, "data", Map.of("id", 77, "nome", "Ana Lima", "email", novo,
                        "emailAnterior", destino)));
        JsonNode troca = esperarEmail(novo);
        assertThat(troca.path("Subject").asText()).isEqualTo("Seu e-mail foi atualizado na WC Autopeças");
    }

    @Test
    void alertasDosTopicosRespeitamAFiltragem() throws Exception {
        String produto = "Produto " + UUID.randomUUID().toString().substring(0, 6);
        int antes = emailsPara("compras@wcautopecas.local").size();
        Map<String, Object> critico = new LinkedHashMap<>();
        critico.put("MessageType", "low_stock_alert");
        critico.put("codigo", "PF-9");
        critico.put("nome", produto);
        critico.put("estoqueAtual", 1);
        critico.put("estoqueMinimo", 10);
        critico.put("status", "critico");
        Map<String, Object> baixo = new LinkedHashMap<>(critico);
        baixo.put("nome", produto + " baixo");
        baixo.put("status", "baixo");
        publisher.publicarTopico("low-stock-alerts-topic", "low_stock_alert", baixo, Map.of("status", "baixo"));
        publisher.publicarTopico("low-stock-alerts-topic", "low_stock_alert", critico, Map.of("status", "critico"));

        String mensagem = "falha " + UUID.randomUUID();
        publisher.publicarTopico("system-notifications-topic", "warning", Map.of("MessageType", "warning",
                "service", "product-service", "severity", "low", "message", mensagem + " low"), Map.of("severity", "low"));
        publisher.publicarTopico("system-notifications-topic", "fiscal-error", Map.of("MessageType", "fiscal-error",
                "service", "fiscal-service", "severity", "critical", "message", mensagem), Map.of("severity", "critical"));

        String cliente = unico("cliente");
        publisher.publicarTopico("sales-notifications-topic", "sale_completed", Map.of("MessageType",
                "sale_completed", "numero", "000099", "clienteNome", "Cliente", "clienteEmail", cliente,
                "valorTotal", 1500.0), Map.of());

        assertThat(html(esperarEmail(cliente))).contains("R$ 1.500,00");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<JsonNode> ops = emailsPara("ops@wcautopecas.local").stream()
                    .filter(e -> html(e).contains(mensagem)).toList();
            assertThat(ops).hasSize(1);
            assertThat(html(ops.get(0))).doesNotContain(mensagem + " low");
            List<JsonNode> compras = emailsPara("compras@wcautopecas.local").stream()
                    .filter(e -> e.path("Subject").asText().startsWith("Estoque crítico: " + produto)).toList();
            assertThat(compras).hasSize(1);
            assertThat(compras.get(0).path("Subject").asText()).isEqualTo("Estoque crítico: " + produto);
        });
        assertThat(emailsPara("compras@wcautopecas.local").size()).isEqualTo(antes + 1);
    }
}
