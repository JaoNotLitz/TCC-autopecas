package br.com.wcautopecas.auth;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.auth")
public class AuthProperties {

    private String tabelaUsuarios = "users";
    /** Falhas seguidas que bloqueiam o usuário. */
    private int maxTentativas = 5;
    /** Tempo de bloqueio e janela do contador de falhas. */
    private Duration bloqueio = Duration.ofMinutes(15);
    private Seed seed = new Seed();

    @Data
    public static class Seed {
        private boolean habilitado;
        private String adminSenha;
        private String vendedorSenha;
    }
}
