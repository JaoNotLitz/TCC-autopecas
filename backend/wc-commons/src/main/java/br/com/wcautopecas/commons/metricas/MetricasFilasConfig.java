package br.com.wcautopecas.commons.metricas;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * HU-B16.3: tempo e resultado de cada mensagem consumida por um {@code @SqsListener}, em todos os
 * serviços que têm listener. O Spring Cloud AWS aplica sozinho qualquer {@link MessageInterceptor}
 * do contexto. Gera {@code wc_listener_seconds{fila}} e {@code wc_mensagens_total{fila,resultado}}.
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnClass(MessageInterceptor.class)
@ConditionalOnBean(MeterRegistry.class)
public class MetricasFilasConfig {

    static final String INICIO = "wcInicioNanos";

    @Bean
    MessageInterceptor<Object> metricasListeners(MeterRegistry registry) {
        return new MessageInterceptor<>() {
            @Override
            public Message<Object> intercept(Message<Object> message) {
                return MessageBuilder.fromMessage(message).setHeader(INICIO, System.nanoTime()).build();
            }

            @Override
            public void afterProcessing(Message<Object> message, Throwable t) {
                String fila = String.valueOf(message.getHeaders().getOrDefault(SqsHeaders.SQS_QUEUE_NAME_HEADER, "?"));
                Counter.builder("wc_mensagens").description("Mensagens consumidas por fila")
                        .tag("fila", fila).tag("resultado", t == null ? "ok" : "falha")
                        .register(registry).increment();
                if (message.getHeaders().get(INICIO) instanceof Long inicio) {
                    Timer.builder("wc_listener").description("Tempo de processamento por listener")
                            .tag("fila", fila).register(registry)
                            .record(Duration.ofNanos(System.nanoTime() - inicio));
                }
            }
        };
    }
}
