package br.com.wcautopecas.fiscal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FiscalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiscalServiceApplication.class, args);
    }
}
