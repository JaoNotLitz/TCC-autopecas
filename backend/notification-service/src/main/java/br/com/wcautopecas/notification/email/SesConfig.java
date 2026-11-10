package br.com.wcautopecas.notification.email;

import br.com.wcautopecas.commons.config.WcProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/** Cliente SES no mesmo esquema dos outros clientes AWS do wc-commons (LocalStack no perfil local). */
@Configuration
public class SesConfig {

    @Bean
    SesClient sesClient(WcProperties wc) {
        var aws = wc.getAws();
        var b = SesClient.builder().region(Region.of(aws.getRegion()));
        if (aws.getEndpoint() != null && !aws.getEndpoint().isBlank()) {
            b.endpointOverride(URI.create(aws.getEndpoint())).credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(aws.getAccessKey(), aws.getSecretKey())));
        } else {
            b.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return b.build();
    }
}
