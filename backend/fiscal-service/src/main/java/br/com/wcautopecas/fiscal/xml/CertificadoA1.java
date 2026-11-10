package br.com.wcautopecas.fiscal.xml;

import br.com.wcautopecas.fiscal.FiscalProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Certificado A1 do secret {@code wc-sefaz-certificate} ({@code {"pfx": base64, "senha": "..."}}),
 * em cache por 1 h (HU-B08.3). A chave privada fica só em memória.
 */
@Component
public class CertificadoA1 {

    public record Credencial(PrivateKey chave, X509Certificate certificado) {

        public Instant validoAte() {
            return certificado.getNotAfter().toInstant();
        }
    }

    private final SecretsManagerClient secrets;
    private final ObjectMapper mapper;
    private final FiscalProperties props;
    private final Clock clock;
    private Credencial atual;
    private Instant carregadoEm;

    public CertificadoA1(SecretsManagerClient secrets, ObjectMapper mapper, FiscalProperties props, Clock clock) {
        this.secrets = secrets;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    public synchronized Credencial get() {
        Instant agora = clock.instant();
        if (atual == null || carregadoEm.plus(props.getCacheCertificado()).isBefore(agora)) {
            atual = carregar(secrets.getSecretValue(r -> r.secretId(props.getSecretCertificado())).secretString());
            carregadoEm = agora;
        }
        return atual;
    }

    public synchronized void esquecer() {
        atual = null;
    }

    static Credencial carregar(String secret) {
        try {
            JsonNode j = new ObjectMapper().readTree(secret);
            char[] senha = j.path("senha").asText().toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(Base64.getMimeDecoder().decode(j.path("pfx").asText())), senha);
            for (String alias : Collections.list(ks.aliases())) {
                if (ks.isKeyEntry(alias)) {
                    return new Credencial((PrivateKey) ks.getKey(alias, senha),
                            (X509Certificate) ks.getCertificate(alias));
                }
            }
            throw new CertificadoException("o .pfx não tem chave privada");
        } catch (CertificadoException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificadoException("não foi possível abrir o certificado: " + e.getMessage());
        }
    }

    /** Dias até vencer (negativo se já venceu). */
    public static long diasRestantes(Credencial c, Instant agora) {
        return Duration.between(agora, c.validoAte()).toDays();
    }

    /** Erro definitivo: certificado ausente, inválido ou vencido. */
    public static class CertificadoException extends RuntimeException {
        public CertificadoException(String message) {
            super(message);
        }
    }
}
