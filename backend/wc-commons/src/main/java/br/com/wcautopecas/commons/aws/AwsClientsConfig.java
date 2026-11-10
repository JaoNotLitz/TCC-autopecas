package br.com.wcautopecas.commons.aws;

import br.com.wcautopecas.commons.config.WcProperties;
import java.net.URI;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Clientes AWS SDK v2. Com {@code wc.aws.endpoint} preenchido (perfil local), todos apontam
 * para o LocalStack com credenciais fixas de teste.
 */
@AutoConfiguration
@EnableConfigurationProperties(WcProperties.class)
public class AwsClientsConfig {

    private final WcProperties.Aws aws;

    public AwsClientsConfig(WcProperties props) {
        this.aws = props.getAws();
    }

    private boolean local() {
        return aws.getEndpoint() != null && !aws.getEndpoint().isBlank();
    }

    private AwsCredentialsProvider credenciais() {
        return local()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(aws.getAccessKey(), aws.getSecretKey()))
                : DefaultCredentialsProvider.create();
    }

    private <B extends AwsClientBuilder<B, C>, C> C montar(B builder) {
        builder.region(Region.of(aws.getRegion())).credentialsProvider(credenciais());
        if (local()) {
            builder.endpointOverride(URI.create(aws.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamoDbClient dynamoDbClient() {
        return montar(DynamoDbClient.builder());
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqsClient sqsClient() {
        return montar(SqsClient.builder());
    }

    @Bean
    @ConditionalOnMissingBean
    public SnsClient snsClient() {
        return montar(SnsClient.builder());
    }

    @Bean
    @ConditionalOnMissingBean
    public S3Client s3Client() {
        // path-style: o LocalStack não resolve bucket.localhost na rede do Compose.
        return montar(S3Client.builder().forcePathStyle(local()));
    }

    @Bean
    @ConditionalOnMissingBean
    public SecretsManagerClient secretsManagerClient() {
        return montar(SecretsManagerClient.builder());
    }
}
