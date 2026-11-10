package br.com.wcautopecas.auth.usuario;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** Item da tabela {@code users}. Datas em ISO 8601 (String). */
@Data
@DynamoDbBean
public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private String papel;
    private Boolean ativo;
    private String ultimoLogin;
    private String bloqueadoAte;
    private String criadoEm;

    @DynamoDbPartitionKey
    public Long getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() {
        return email;
    }
}
