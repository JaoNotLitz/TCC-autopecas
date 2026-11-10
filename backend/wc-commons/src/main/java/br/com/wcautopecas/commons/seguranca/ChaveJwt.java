package br.com.wcautopecas.commons.seguranca;

import br.com.wcautopecas.commons.config.WcProperties;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Chave HS512 compartilhada por auth-service, filtros e Lambda Authorizer. Lida do secret
 * {@code wc-jwt-secret} na primeira vez e mantida em memória.
 */
public class ChaveJwt {

    private final WcProperties.Jwt config;
    private final SecretsManagerClient secrets;
    private volatile SecretKey chave;

    public ChaveJwt(WcProperties.Jwt config, SecretsManagerClient secrets) {
        this.config = config;
        this.secrets = secrets;
    }

    public SecretKey get() {
        SecretKey atual = chave;
        if (atual == null) {
            synchronized (this) {
                if (chave == null) {
                    chave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(lerBase64().trim()));
                }
                atual = chave;
            }
        }
        return atual;
    }

    private String lerBase64() {
        if (config.getSecret() != null && !config.getSecret().isBlank()) {
            return config.getSecret();
        }
        return secrets.getSecretValue(r -> r.secretId(config.getSecretName())).secretString();
    }
}
