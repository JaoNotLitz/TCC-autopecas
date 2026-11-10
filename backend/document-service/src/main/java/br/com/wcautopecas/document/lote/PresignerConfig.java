package br.com.wcautopecas.document.lote;

import br.com.wcautopecas.commons.config.WcProperties;
import br.com.wcautopecas.document.DocumentosProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Link pré-assinado do lote do contador; no local aponta para o LocalStack visto do host. */
@Configuration
public class PresignerConfig {

    @Bean
    S3Presigner s3Presigner(WcProperties wc, DocumentosProperties props) {
        var aws = wc.getAws();
        boolean local = aws.getEndpoint() != null && !aws.getEndpoint().isBlank();
        var b = S3Presigner.builder().region(Region.of(aws.getRegion()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(local).build());
        if (local) {
            b.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(aws.getAccessKey(), aws.getSecretKey())));
            String publico = props.getS3EnderecoPublico();
            b.endpointOverride(URI.create(publico == null || publico.isBlank() ? aws.getEndpoint() : publico));
        } else {
            b.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return b.build();
    }
}
