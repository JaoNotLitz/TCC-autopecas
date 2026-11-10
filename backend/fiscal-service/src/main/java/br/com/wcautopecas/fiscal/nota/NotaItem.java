package br.com.wcautopecas.fiscal.nota;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/** Item da tabela {@code notas_fiscais} (seção 3.4). */
@Data
@DynamoDbBean
public class NotaItem {

    private Long id;
    /** NFE | NFCE | DEVOLUCAO | ENTRADA */
    private String tipo;
    /** 55 | 65 */
    private String modelo;
    private String serie;
    /** 6 dígitos, ex.: 000126 */
    private String numero;
    private String chaveAcesso;
    /** processando | autorizada | cancelada | rejeitada */
    private String status;
    private Long vendaId;
    private Long clienteId;
    private String clienteNome;
    private String clienteDocumento;
    private String clienteUf;
    private String clienteCidade;
    private String clienteEmail;
    /** Da venda, para o grupo {@code pag} e o {@code indPres} do XML. */
    private String metodoPagamento;
    private String bandeira;
    private String modalidade;
    private Long fornecedorId;
    private Long notaReferenciadaId;
    private String chaveReferenciada;
    private String motivo;
    private BigDecimal valorProdutos;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;
    private String emitidaEm;
    private String autorizadaEm;
    private String canceladaEm;
    private String ambiente;
    private Integer tentativasEnvio;
    private String protocolo;
    private String codigoStatus;
    private String motivoRejeicao;
    private String xmlPath;
    private String pdfPath;
    /** Preenchido pelo document-service quando a nota vai no lote do contador (HU-B13.4). */
    private String enviadaContadorEm;
    /** Evento de cancelamento em andamento (a nota fica "processando" até a SEFAZ responder). */
    private Long cancelamentoId;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "emitidaEm-index")
    public String getTipo() {
        return tipo;
    }

    @DynamoDbSecondarySortKey(indexNames = "emitidaEm-index")
    public String getEmitidaEm() {
        return emitidaEm;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    public String getStatus() {
        return status;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "venda-index")
    public Long getVendaId() {
        return vendaId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "chave-index")
    public String getChaveAcesso() {
        return chaveAcesso;
    }
}
