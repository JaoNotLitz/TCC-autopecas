package br.com.wcautopecas.entry.entrada;

import br.com.wcautopecas.commons.dynamo.ListagemDynamo;
import br.com.wcautopecas.entry.EntradasProperties;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class EntradaRepository {

    static final TableSchema<NotaEntrada> ENTRADA = TableSchema.fromBean(NotaEntrada.class);
    static final TableSchema<Fornecedor> FORNECEDOR = TableSchema.fromBean(Fornecedor.class);

    private final DynamoDbTable<NotaEntrada> entradas;
    private final DynamoDbTable<Fornecedor> fornecedores;

    public EntradaRepository(DynamoDbEnhancedClient enhanced, EntradasProperties props) {
        this.entradas = enhanced.table(props.getTabelaEntradas(), ENTRADA);
        this.fornecedores = enhanced.table(props.getTabelaFornecedores(), FORNECEDOR);
    }

    public void criar(NotaEntrada e) {
        entradas.putItem(r -> r.item(e).conditionExpression(Expression.builder()
                .expression("attribute_not_exists(id)").build()));
    }

    public Optional<NotaEntrada> porId(long id) {
        return Optional.ofNullable(entradas.getItem(Key.builder().partitionValue(id).build()));
    }

    public Optional<NotaEntrada> porChave(String chave) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(chave).build());
        return ListagemDynamo.todos(entradas.index("chave-index").query(r -> r.queryConditional(cond))).stream()
                .findFirst();
    }

    public List<NotaEntrada> todas() {
        return ListagemDynamo.todos(entradas.scan());
    }

    public List<NotaEntrada> doFornecedor(long fornecedorId) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(fornecedorId).build());
        return ListagemDynamo.todos(entradas.index("fornecedor-index").query(r -> r.queryConditional(cond)));
    }

    public Optional<Fornecedor> fornecedor(String cnpj) {
        return Optional.ofNullable(fornecedores.getItem(r -> r.key(Key.builder().partitionValue(cnpj).build())
                .consistentRead(true)));
    }

    /** Cria só se o CNPJ ainda não existe; {@code false} se outra requisição criou antes. */
    public boolean criarFornecedor(Fornecedor f) {
        try {
            fornecedores.putItem(r -> r.item(f).conditionExpression(Expression.builder()
                    .expression("attribute_not_exists(cnpj)").build()));
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public void salvarFornecedor(Fornecedor f) {
        fornecedores.putItem(f);
    }

    public List<Fornecedor> fornecedores() {
        return ListagemDynamo.todos(fornecedores.scan());
    }
}
