package br.com.wcautopecas.commons.mensageria;

import br.com.wcautopecas.commons.aws.AwsClientsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@AutoConfiguration(after = {AwsClientsConfig.class, RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
@org.springframework.scheduling.annotation.EnableScheduling
public class MensageriaConfig {

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(SqsClient sqs, SnsClient sns, ObjectMapper mapper) {
        return new EventPublisher(sqs, sns, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public Outbox outbox(EventPublisher publisher, StringRedisTemplate redis, ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Value("${spring.application.name:servico}") String servico) {
        return new Outbox(publisher, redis, mapper, servico);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyGuard idempotencyGuard(StringRedisTemplate redis) {
        return new IdempotencyGuard(redis);
    }
}
