package br.com.wcautopecas.entry.entrada;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** Item da tabela {@code entry_notes}: a nota do fornecedor como foi lançada, com os itens embutidos. */
@Data
@DynamoDbBean
public class NotaEntrada {

    private Long id;
    /** registrada; na importação assíncrona também recebida, com_pendencias e erro */
    private String status;
    /** manual | xml */
    private String origem;
    private Long fornecedorId;
    private String fornecedorCnpj;
    private String fornecedorNome;
    private String numero;
    private String serie;
    private String chaveAcesso;
    private String naturezaOperacao;
    private String dataEmissao;
    private String dataEntrada;
    private BigDecimal valorTotal;
    private Long notaFiscalId;
    private Long usuarioId;
    private String registradaEm;
    private List<ItemEntrada> itens;
    /** Importação assíncrona: onde o XML está no {@code wc-xml-storage}. */
    private String s3Key;
    /** Motivo quando {@code status = erro}. */
    private String erro;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "fornecedor-index")
    public Long getFornecedorId() {
        return fornecedorId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "data-index")
    public String getDataEntrada() {
        return dataEntrada;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "chave-index")
    public String getChaveAcesso() {
        return chaveAcesso;
    }
}
