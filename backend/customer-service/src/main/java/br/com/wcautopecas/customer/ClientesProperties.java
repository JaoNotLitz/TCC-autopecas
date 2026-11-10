package br.com.wcautopecas.customer;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.clientes")
public class ClientesProperties {

    private String tabela = "customers";
    private String filaEventos = "customer-events-queue";
    private String filaVendas = "customer-sales-events-queue";
    private Duration cacheBusca = Duration.ofMinutes(5);
    private Duration cacheCliente = Duration.ofHours(1);
}
