package br.com.wcautopecas.customer.cliente;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Item da tabela {@code customers}. Campos vazios são gravados como ausentes (o DynamoDB não
 * aceita string vazia em chave de GSI). {@code nomeBusca}, {@code status}, {@code valorTotalCompras}
 * e as datas de auditoria são internos e não vão para o front.
 */
@Data
@DynamoDbBean
public class ClienteItem {

    private Long id;
    private String tipo;
    private String documento;
    private String nome;
    private String nomeBusca;
    private String email;
    private String telefone;
    private String cidade;
    private String uf;
    /** Endereço opcional (8 dígitos no CEP), usado no {@code enderDest} da NF-e. */
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private Boolean ativo;
    /** "ativo" | "inativo", chave do GSI status-index. */
    private String status;
    private Long totalCompras;
    private Double valorTotalCompras;
    private String ultimaCompra;
    private String dataCriacao;
    private String dataAtualizacao;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "documento-index")
    public String getDocumento() {
        return documento;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() {
        return email;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "status-index")
    public String getStatus() {
        return status;
    }
}
