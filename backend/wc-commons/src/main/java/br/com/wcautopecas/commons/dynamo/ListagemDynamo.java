package br.com.wcautopecas.commons.dynamo;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

/**
 * O front não pagina: toda listagem é um array completo. Este utilitário percorre todas as
 * páginas do DynamoDB (seguindo o LastEvaluatedKey) e devolve uma lista só.
 */
public final class ListagemDynamo {

    private ListagemDynamo() {
    }

    public static <T> List<T> todos(SdkIterable<Page<T>> paginas) {
        List<T> itens = new ArrayList<>();
        for (Page<T> pagina : paginas) {
            itens.addAll(pagina.items());
        }
        return itens;
    }
}
