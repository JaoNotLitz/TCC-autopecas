package br.com.wcautopecas.fiscal.evento;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** Item da tabela {@code fiscal_events}: cancelamento ou inutilização pedidos à SEFAZ. */
@Data
@DynamoDbBean
public class EventoFiscal {

    public static final String CANCELAMENTO = "CANCELAMENTO";
    public static final String INUTILIZACAO = "INUTILIZACAO";

    private Long id;
    private String tipo;
    /** processando | homologado | rejeitado */
    private String status;
    private Long notaId;
    private String modelo;
    private String serie;
    private Long numeroInicial;
    private Long numeroFinal;
    private String justificativa;
    private Long usuarioId;
    private String registradoEm;
    private String protocolo;
    private String motivoRejeicao;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "tipo-index")
    public String getTipo() {
        return tipo;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "nota-index")
    public Long getNotaId() {
        return notaId;
    }
}
