package br.com.wcautopecas.auth.usuario;

import br.com.wcautopecas.auth.AuthProperties;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class UsuarioRepository {

    private final DynamoDbTable<Usuario> tabela;

    public UsuarioRepository(DynamoDbEnhancedClient enhanced, AuthProperties props) {
        this.tabela = enhanced.table(props.getTabelaUsuarios(), TableSchema.fromBean(Usuario.class));
    }

    public Optional<Usuario> porId(long id) {
        return Optional.ofNullable(tabela.getItem(Key.builder().partitionValue(id).build()));
    }

    /** Busca pelo GSI {@code email-index}; e-mails são gravados em minúsculas. */
    public Optional<Usuario> porEmail(String email) {
        var cond = QueryConditional.keyEqualTo(Key.builder().partitionValue(email.trim().toLowerCase()).build());
        return tabela.index("email-index").query(r -> r.queryConditional(cond)).stream()
                .flatMap(p -> p.items().stream())
                .findFirst();
    }

    public void salvar(Usuario usuario) {
        tabela.putItem(usuario);
    }
}
