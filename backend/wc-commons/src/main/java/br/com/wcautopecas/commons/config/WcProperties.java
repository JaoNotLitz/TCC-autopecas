package br.com.wcautopecas.commons.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurações comuns (prefixo {@code wc}). */
@Data
@ConfigurationProperties(prefix = "wc")
public class WcProperties {

    private Aws aws = new Aws();
    private Jwt jwt = new Jwt();
    private Seguranca seguranca = new Seguranca();
    private Cors cors = new Cors();

    @Data
    public static class Aws {
        /** Endpoint do LocalStack (ex.: http://localstack:4566). Vazio = AWS real. */
        private String endpoint;
        private String region = "sa-east-1";
        private String accessKey = "test";
        private String secretKey = "test";
        /** Nome da tabela de contadores de ids. */
        private String tabelaCounters = "counters";
    }

    @Data
    public static class Jwt {
        /** Nome do secret com a chave HS512 em Base64. */
        private String secretName = "wc-jwt-secret";
        /** Chave em Base64 informada direto (testes). Se preenchida, o Secrets Manager não é consultado. */
        private String secret;
        private Duration validade = Duration.ofHours(8);
    }

    @Data
    public static class Seguranca {
        /** Liga o filtro de autenticação. */
        private boolean habilitada = true;
        /** Caminhos sem token (padrões Ant). */
        private List<String> caminhosPublicos = new ArrayList<>(List.of(
                "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/internal/**"));
    }

    @Data
    public static class Cors {
        private List<String> origens = new ArrayList<>(List.of("http://localhost:5173"));
    }
}
