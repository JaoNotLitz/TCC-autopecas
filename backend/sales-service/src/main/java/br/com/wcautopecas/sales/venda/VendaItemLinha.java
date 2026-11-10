package br.com.wcautopecas.sales.venda;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** Item da tabela {@code sale_items} (PK vendaId, SK itemId = posição na venda). */
@Data
@DynamoDbBean
public class VendaItemLinha {

    private Long vendaId;
    private Long itemId;
    private Long produtoId;
    private String codigo;
    private String nome;
    private Long quantidade;
    private BigDecimal valorUnitario;

    @DynamoDbPartitionKey
    public Long getVendaId() {
        return vendaId;
    }

    @DynamoDbSortKey
    public Long getItemId() {
        return itemId;
    }
}
