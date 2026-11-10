package br.com.wcautopecas.commons.dynamo;

import br.com.wcautopecas.commons.aws.AwsClientsConfig;
import br.com.wcautopecas.commons.config.WcProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@AutoConfiguration(after = AwsClientsConfig.class)
public class DynamoConfig {

    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(DynamoDbClient dynamo, WcProperties props) {
        return new IdGenerator(dynamo, props.getAws().getTabelaCounters());
    }
}
