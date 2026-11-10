package br.com.wcautopecas.fiscal.xml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/** Gera .pfx de teste com o keytool do JDK (válido ou já vencido). */
public final class CertificadosTeste {

    public static final String SENHA = "teste123";

    private CertificadosTeste() {
    }

    /** Secret no formato do {@code wc-sefaz-certificate}. */
    public static String secret(boolean vencido) {
        try {
            Path dir = Files.createTempDirectory("a1");
            Path pfx = dir.resolve("a1.pfx");
            String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
            var cmd = new java.util.ArrayList<>(java.util.List.of(keytool, "-genkeypair", "-alias", "wc-a1",
                    "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12", "-keystore", pfx.toString(),
                    "-storepass", SENHA, "-keypass", SENHA,
                    "-dname", "CN=WC AUTOPECAS LTDA:12345678000195, O=ICP-Brasil, C=BR"));
            if (vencido) {
                cmd.addAll(java.util.List.of("-startdate", "-2y", "-validity", "30"));
            } else {
                cmd.addAll(java.util.List.of("-validity", "365"));
            }
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String saida = new String(p.getInputStream().readAllBytes());
            if (p.waitFor() != 0) {
                throw new IllegalStateException("keytool falhou: " + saida);
            }
            String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(pfx));
            Files.delete(pfx);
            Files.delete(dir);
            return "{\"pfx\":\"" + b64 + "\",\"senha\":\"" + SENHA + "\"}";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
