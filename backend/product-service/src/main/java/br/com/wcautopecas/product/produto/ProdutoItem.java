package br.com.wcautopecas.product.produto;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** Item da tabela {@code products} (cadastro). O estoque fica em {@link EstoqueItem}. */
@Data
@DynamoDbBean
public class ProdutoItem {

    private Long id;
    private String codigo;
    private String nome;
    private String nomeBusca;
    private String ncm;
    private Long estoqueMinimo;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;
    private String cfop;
    private String origem;
    private String cstIcms;
    private BigDecimal aliquotaIcms;
    private Boolean ativo;
    private String dataCriacao;
    private String dataAtualizacao;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "codigo-index")
    public String getCodigo() {
        return codigo;
    }
}
