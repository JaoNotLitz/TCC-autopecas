package br.com.wcautopecas.fiscal.nota;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Item da tabela {@code notas_items}. O saldo de devolução fica na <b>primeira linha</b> de cada
 * produto ({@code quantidadeDevolvida} e {@code quantidadeEmDevolucao} somam o produto inteiro).
 */
@Data
@DynamoDbBean
public class NotaLinha {

    private Long notaId;
    private Long itemId;
    private Long produtoId;
    private String codigo;
    private String descricao;
    private String ncm;
    private String cfop;
    private String origem;
    private String cstIcms;
    private BigDecimal aliquotaIcms;
    private Long quantidade;
    private BigDecimal valorUnitario;
    private Long quantidadeDevolvida;
    private Long quantidadeEmDevolucao;

    @DynamoDbPartitionKey
    public Long getNotaId() {
        return notaId;
    }

    @DynamoDbSortKey
    public Long getItemId() {
        return itemId;
    }
}
