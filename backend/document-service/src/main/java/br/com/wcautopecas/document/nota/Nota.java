package br.com.wcautopecas.document.nota;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Recorte da tabela {@code notas_fiscais} (dona: fiscal-service). Só leitura; as duas colunas que
 * este serviço grava ({@code pdfPath}, {@code enviadaContadorEm}) vão por UpdateItem, sem regravar o item.
 */
@Data
@DynamoDbBean
public class Nota {

    private Long id;
    private String tipo;
    private String modelo;
    private String serie;
    private String numero;
    private String chaveAcesso;
    private String status;
    private BigDecimal valorTotal;
    private String emitidaEm;
    private String autorizadaEm;
    private String canceladaEm;
    private String xmlPath;
    private String pdfPath;
    private String enviadaContadorEm;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }
}
