package br.com.wcautopecas.customer.cliente;

import br.com.wcautopecas.commons.config.WcProperties;
import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.customer.ClientesProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Acesso à tabela {@code customers}. A unicidade de documento e e-mail é garantida por
 * itens-guarda gravados na mesma transação do cliente, com {@code attribute_not_exists}. Como a
 * chave de {@code customers} é numérica, os guardas ficam na tabela {@code counters} (chave texto),
 * com o prefixo {@code unico:clientes:}.
 */
@Repository
public class ClienteRepository {

    static final TableSchema<ClienteItem> SCHEMA = TableSchema.fromBean(ClienteItem.class);

    private final DynamoDbTable<ClienteItem> tabela;
    private final DynamoDbClient dynamo;
    private final String nomeTabela;
    private final String tabelaGuardas;

    public ClienteRepository(DynamoDbEnhancedClient enhanced, DynamoDbClient dynamo, ClientesProperties props,
            WcProperties wc) {
        this.tabela = enhanced.table(props.getTabela(), SCHEMA);
        this.dynamo = dynamo;
        this.nomeTabela = props.getTabela();
        this.tabelaGuardas = wc.getAws().getTabelaCounters();
    }

    public Optional<ClienteItem> porId(long id) {
        return Optional.ofNullable(tabela.getItem(Key.builder().partitionValue(id).build()));
    }

    public Optional<ClienteItem> porDocumento(String documento) {
        return porIndice("documento-index", documento).stream().findFirst();
    }

    public Optional<ClienteItem> porEmail(String email) {
        return porIndice("email-index", email).stream().findFirst();
    }

    public List<ClienteItem> todos() {
        return ListagemDynamo.todos(tabela.scan());
    }

    private List<ClienteItem> porIndice(String indice, String valor) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(valor).build());
        return ListagemDynamo.todos(tabela.index(indice).query(r -> r.queryConditional(cond)));
    }

    /** Grava um cliente novo com os guardas de documento e e-mail. */
    public void criar(ClienteItem novo) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(putCliente(novo));
        ops.add(putGuarda("documento", novo.getDocumento(), novo.getId()));
        if (novo.getEmail() != null) {
            ops.add(putGuarda("email", novo.getEmail(), novo.getId()));
        }
        executar(ops);
    }

    /** Regrava o cliente trocando os guardas que mudaram. */
    public void atualizar(ClienteItem antigo, ClienteItem novo) {
        List<TransactWriteItem> ops = new ArrayList<>();
        ops.add(putCliente(novo));
        trocarGuarda(ops, "documento", antigo.getDocumento(), novo.getDocumento(), novo.getId());
        trocarGuarda(ops, "email", antigo.getEmail(), novo.getEmail(), novo.getId());
        executar(ops);
    }

    /** Grava sem mexer nos guardas (documento e e-mail não mudaram). */
    public void salvar(ClienteItem item) {
        tabela.putItem(item);
    }

    /**
     * Estatística de compras com UpdateItem atômico. {@code ultimaCompra} só avança. Devolve
     * {@code false} se o cliente não existe.
     */
    public boolean registrarCompra(long id, double valor, String quando) {
        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(nomeTabela)
                    .key(Map.of("id", AttributeValue.fromN(Long.toString(id))))
                    .conditionExpression("attribute_exists(id)")
                    .updateExpression("ADD totalCompras :um, valorTotalCompras :valor SET dataAtualizacao = :agora")
                    .expressionAttributeValues(Map.of(
                            ":um", AttributeValue.fromN("1"),
                            ":valor", AttributeValue.fromN(Double.toString(valor)),
                            ":agora", AttributeValue.fromS(quando)))
                    .returnValues(ReturnValue.NONE)
                    .build());
        } catch (software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException e) {
            return false;
        }
        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(nomeTabela)
                    .key(Map.of("id", AttributeValue.fromN(Long.toString(id))))
                    .conditionExpression("attribute_not_exists(ultimaCompra) OR ultimaCompra < :quando")
                    .updateExpression("SET ultimaCompra = :quando")
                    .expressionAttributeValues(Map.of(":quando", AttributeValue.fromS(quando)))
                    .build());
        } catch (software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException e) {
            // já existe uma compra mais recente
        }
        return true;
    }

    private TransactWriteItem putCliente(ClienteItem item) {
        return TransactWriteItem.builder().put(Put.builder()
                .tableName(nomeTabela)
                .item(SCHEMA.itemToMap(item, true))
                .build()).build();
    }

    private TransactWriteItem putGuarda(String campo, String valor, long clienteId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("recurso", AttributeValue.fromS(chaveGuarda(campo, valor)));
        item.put("clienteId", AttributeValue.fromN(Long.toString(clienteId)));
        return TransactWriteItem.builder().put(Put.builder()
                .tableName(tabelaGuardas)
                .item(item)
                .conditionExpression("attribute_not_exists(recurso) OR clienteId = :id")
                .expressionAttributeValues(Map.of(":id", AttributeValue.fromN(Long.toString(clienteId))))
                .build()).build();
    }

    private void trocarGuarda(List<TransactWriteItem> ops, String campo, String antigo, String novo, long id) {
        if (Objects.equals(antigo, novo)) {
            return;
        }
        if (antigo != null) {
            ops.add(TransactWriteItem.builder().delete(Delete.builder()
                    .tableName(tabelaGuardas)
                    .key(Map.of("recurso", AttributeValue.fromS(chaveGuarda(campo, antigo))))
                    .build()).build());
        }
        if (novo != null) {
            ops.add(putGuarda(campo, novo, id));
        }
    }

    static String chaveGuarda(String campo, String valor) {
        return "unico:clientes:" + campo + ":" + valor;
    }

    private void executar(List<TransactWriteItem> ops) {
        try {
            dynamo.transactWriteItems(r -> r.transactItems(ops));
        } catch (TransactionCanceledException e) {
            throw traduzir(e, ops);
        }
    }

    /** Descobre qual guarda falhou para devolver a mensagem certa. */
    private RuntimeException traduzir(TransactionCanceledException e, List<TransactWriteItem> ops) {
        List<CancellationReason> motivos = e.cancellationReasons();
        for (int i = 0; i < motivos.size() && i < ops.size(); i++) {
            if (!"ConditionalCheckFailed".equals(motivos.get(i).code()) || ops.get(i).put() == null) {
                continue;
            }
            AttributeValue recurso = ops.get(i).put().item().get("recurso");
            if (recurso != null && recurso.s().startsWith("unico:clientes:email:")) {
                return new ConflitoException("E-mail já cadastrado");
            }
            return new ConflitoException("Documento já cadastrado");
        }
        return e;
    }
}
