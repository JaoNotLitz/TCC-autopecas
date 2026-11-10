package br.com.wcautopecas.product.estoque;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/** Item da tabela {@code inventory_movements}. {@code quantidade} tem sinal. */
@Data
@DynamoDbBean
public class MovimentoItem {

    private Long id;
    private Long produtoId;
    /** entrada | saida | ajuste | reserva | liberacao */
    private String tipo;
    private Long quantidade;
    private Long estoqueAnterior;
    private Long estoqueNovo;
    private Long referenciaId;
    private String referenciaType;
    private Long usuarioId;
    private String motivo;
    private String data;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "produto-data-index")
    public Long getProdutoId() {
        return produtoId;
    }

    @DynamoDbSecondarySortKey(indexNames = "produto-data-index")
    public String getData() {
        return data;
    }
}
