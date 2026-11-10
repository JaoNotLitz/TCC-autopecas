package br.com.wcautopecas.commons.seguranca;

import br.com.wcautopecas.commons.aws.AwsClientsConfig;
import br.com.wcautopecas.commons.config.WcProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@AutoConfiguration(after = {AwsClientsConfig.class, RedisAutoConfiguration.class})
public class SegurancaConfig implements WebMvcConfigurer {

    private final WcProperties props;

    public SegurancaConfig(WcProperties props) {
        this.props = props;
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChaveJwt chaveJwt(SecretsManagerClient secrets) {
        return new ChaveJwt(props.getJwt(), secrets);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(ChaveJwt chave, Clock clock) {
        return new JwtService(chave, props.getJwt().getValidade(), clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessaoStore sessaoStore(StringRedisTemplate redis) {
        return new SessaoStore(redis);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenServico tokenServico(JwtService jwt, SessaoStore sessoes, Clock clock) {
        return new TokenServico(jwt, sessoes, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "wc.seguranca", name = "habilitada", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AutenticacaoFilter> autenticacaoFilter(JwtService jwt, SessaoStore sessoes,
            ObjectMapper mapper) {
        var reg = new FilterRegistrationBean<>(
                new AutenticacaoFilter(jwt, sessoes, props.getSeguranca().getCaminhosPublicos(), mapper));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PapelInterceptor());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UsuarioArgumentResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(props.getCors().getOrigens().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Correlation-Id")
                .exposedHeaders("Content-Disposition", "X-Correlation-Id")
                .maxAge(3600);
    }
}
