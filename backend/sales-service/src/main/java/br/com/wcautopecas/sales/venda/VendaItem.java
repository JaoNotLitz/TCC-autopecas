package br.com.wcautopecas.sales.venda;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/** Item da tabela {@code sales}. {@code data} = criadaEm (GSI data-index). */
@Data
@DynamoDbBean
public class VendaItem {

    private Long id;
    private String numero;
    private String tipoDocumento;
    private String modalidade;
    private Long clienteId;
    private String metodoPagamento;
    private String bandeira;
    private String destinoDinheiro;
    private BigDecimal desconto;
    private BigDecimal subtotal;
    private BigDecimal total;
    /** orcamento | finalizada | cancelada */
    private String status;
    private Long vendedorId;
    private Long notaFiscalId;
    private String data;
    private String finalizadaEm;
    private String canceladaEm;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "cliente-index")
    public Long getClienteId() {
        return clienteId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"status-index", "data-index"})
    public String getStatus() {
        return status;
    }

    @DynamoDbSecondarySortKey(indexNames = "data-index")
    public String getData() {
        return data;
    }
}
