package br.com.wcautopecas.commons.teste;

import java.net.URI;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Clientes "crus" apontando para os containers do {@link InfraTeste}, para testes sem Spring. */
public final class ClientesTeste {

    private ClientesTeste() {
    }

    public static DynamoDbClient dynamo() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(InfraTeste.endpoint()))
                .region(Region.SA_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }

    public static StringRedisTemplate redis() {
        var cfg = new RedisStandaloneConfiguration(InfraTeste.REDIS.getHost(), InfraTeste.REDIS.getMappedPort(6379));
        cfg.setPassword(InfraTeste.REDIS_SENHA);
        var factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        factory.start();
        return new StringRedisTemplate(factory);
    }
}
