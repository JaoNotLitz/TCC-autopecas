package br.com.wcautopecas.entry.entrada;

import java.math.BigDecimal;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@DynamoDbBean
public class ItemEntrada {

    private Long produtoId;
    private String codigo;
    private String descricao;
    private String ncm;
    private String cfop;
    private Long quantidade;
    private BigDecimal valorUnitario;
}
