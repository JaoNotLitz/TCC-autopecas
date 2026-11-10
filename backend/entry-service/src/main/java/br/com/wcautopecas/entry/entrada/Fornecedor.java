package br.com.wcautopecas.entry.entrada;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** Tabela {@code suppliers}: chave {@code cnpj}, {@code id} numérico para as notas. */
@Data
@DynamoDbBean
public class Fornecedor {

    private String cnpj;
    private Long id;
    private String razaoSocial;
    private String ie;
    private String uf;
    private String criadoEm;
    private String atualizadoEm;

    @DynamoDbPartitionKey
    public String getCnpj() {
        return cnpj;
    }
}
