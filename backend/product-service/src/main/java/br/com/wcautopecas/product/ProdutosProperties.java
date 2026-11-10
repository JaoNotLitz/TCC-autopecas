package br.com.wcautopecas.product;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.produtos")
public class ProdutosProperties {

    private String tabelaProdutos = "products";
    private String tabelaEstoque = "inventory";
    private String tabelaMovimentos = "inventory_movements";
    private String filaEstoque = "inventory-updates-queue";
    private String topicoAlertas = "low-stock-alerts-topic";
    private String topicoSistema = "system-notifications-topic";
    private Duration cacheProduto = Duration.ofHours(1);
    private Duration cacheResumo = Duration.ofMinutes(5);
    private FiscalPadrao fiscalPadrao = new FiscalPadrao();

    @Data
    public static class FiscalPadrao {
        private String cfop = "5102";
        private String origem = "0";
        private String cstIcms = "00";
        private BigDecimal aliquotaIcms = BigDecimal.valueOf(18);
    }
}
