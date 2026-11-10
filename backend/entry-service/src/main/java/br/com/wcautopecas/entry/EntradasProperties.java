package br.com.wcautopecas.entry;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.entradas")
public class EntradasProperties {

    private String tabelaEntradas = "entry_notes";
    private String tabelaFornecedores = "suppliers";
    private String filaEstoque = "inventory-updates-queue";
    private String productUrl = "http://product-service:8082";
    private String fiscalUrl = "http://fiscal-service:8084";
    private Duration timeoutChamadas = Duration.ofSeconds(2);
}
