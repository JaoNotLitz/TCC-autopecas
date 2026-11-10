package br.com.wcautopecas.commons.teste;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Containers compartilhados pelos testes de integração de todos os módulos: um LocalStack com
 * os mesmos scripts de init do Compose ({@code infra/local/init}) e um Redis. Sobem uma vez por JVM.
 */
public final class InfraTeste {

    public static final String LOCALSTACK_IMAGE = "localstack/localstack:3.8.1";
    public static final String REDIS_SENHA = "teste";
    /** Chave HS512 fixa dos testes (64 bytes em Base64). */
    public static final String JWT_SECRET = InfraTesteChaves.JWT_SECRET;

    public static final GenericContainer<?> LOCALSTACK = new GenericContainer<>(DockerImageName.parse(LOCALSTACK_IMAGE))
            .withExposedPorts(4566)
            .withEnv("SERVICES", "dynamodb,sqs,sns,s3,secretsmanager,ses,sts,iam")
            .withEnv("AWS_DEFAULT_REGION", "sa-east-1")
            .withCopyFileToContainer(MountableFile.forHostPath(dirInit()), "/etc/localstack/init")
            .waitingFor(Wait.forLogMessage(".*\\[wc-init\\] init concluído.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    public static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_SENHA);

    static {
        LOCALSTACK.start();
        REDIS.start();
    }

    private InfraTeste() {
    }

    public static String endpoint() {
        return "http://" + LOCALSTACK.getHost() + ":" + LOCALSTACK.getMappedPort(4566);
    }

    /** Propriedades Spring para apontar o serviço para os containers. */
    public static void registrar(DynamicPropertyRegistry r) {
        r.add("wc.aws.endpoint", InfraTeste::endpoint);
        r.add("wc.aws.region", () -> "sa-east-1");
        r.add("wc.jwt.secret", () -> JWT_SECRET);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("spring.data.redis.password", () -> REDIS_SENHA);
    }

    /** Procura {@code infra/local/init} subindo a partir do diretório do módulo. */
    static Path dirInit() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            Path init = p.resolve("infra/local/init");
            if (Files.isDirectory(init)) {
                return init;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("infra/local/init não encontrado");
    }
}
