package br.com.wcautopecas.fiscal;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/** O mesmo simulador da SEFAZ do Compose ({@code infra/local/sefaz-mock}), uma vez por JVM. */
public final class SefazMockTeste {

    public static final GenericContainer<?> SEFAZ = new GenericContainer<>("wiremock/wiremock:3.9.2")
            .withCopyFileToContainer(MountableFile.forHostPath(dir()), "/home/wiremock")
            .withCommand("--disable-banner")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

    static {
        SEFAZ.start();
    }

    private SefazMockTeste() {
    }

    public static String url() {
        return "http://" + SEFAZ.getHost() + ":" + SEFAZ.getMappedPort(8080);
    }

    static Path dir() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            Path d = p.resolve("infra/local/sefaz-mock");
            if (Files.isDirectory(d)) {
                return d;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("infra/local/sefaz-mock não encontrado");
    }
}
