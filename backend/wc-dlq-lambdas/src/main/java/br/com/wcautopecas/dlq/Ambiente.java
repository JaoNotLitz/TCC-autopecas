package br.com.wcautopecas.dlq;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/** Configuração lida das variáveis de ambiente da Lambda e os clientes AWS (LocalStack quando há endpoint). */
public record Ambiente(String endpoint, String regiao, String dlq, int limite, String topico, String tabelaCounters) {

    public static Ambiente doSistema() {
        return new Ambiente(System.getenv("AWS_ENDPOINT_URL"), env("AWS_REGION", "sa-east-1"),
                env("QUEUE_FISCAL_DLQ", "fiscal-dlq"), Integer.parseInt(env("DLQ_LIMITE", "5")),
                env("TOPIC_SYSTEM_NOTIFICATIONS", "system-notifications-topic"), env("TABLE_COUNTERS", "counters"));
    }

    static String env(String nome, String padrao) {
        String v = System.getenv(nome);
        return v == null || v.isBlank() ? padrao : v;
    }

    private boolean local() {
        return endpoint != null && !endpoint.isBlank();
    }

    private <B extends AwsClientBuilder<B, C>, C> C montar(B b) {
        b.region(Region.of(regiao));
        if (local()) {
            // O LocalStack injeta AWS_ENDPOINT_URL nas Lambdas; as credenciais são as de teste.
            b.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        } else {
            b.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return b.build();
    }

    public SqsClient sqs() {
        return montar(SqsClient.builder().httpClient(UrlConnectionHttpClient.create()));
    }

    public SnsClient sns() {
        return montar(SnsClient.builder().httpClient(UrlConnectionHttpClient.create()));
    }

    public DynamoDbClient dynamo() {
        return montar(DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()));
    }
}
