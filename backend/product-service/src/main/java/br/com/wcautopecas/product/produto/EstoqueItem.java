package br.com.wcautopecas.product.produto;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** Item da tabela {@code inventory}. {@code versao} faz o controle otimista (HU-B05.6). */
@Data
@DynamoDbBean
public class EstoqueItem {

    private Long produtoId;
    private Long estoqueAtual;
    private Long reservado;
    private Long disponivel;
    private Long estoqueMinimo;
    private String status;
    private Long versao;
    private String ultimaMovimentacao;

    @DynamoDbPartitionKey
    public Long getProdutoId() {
        return produtoId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    public String getStatus() {
        return status;
    }
}
