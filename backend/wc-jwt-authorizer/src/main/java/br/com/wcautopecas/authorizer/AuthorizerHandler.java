package br.com.wcautopecas.authorizer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.IamPolicyResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Lambda Authorizer do tipo REQUEST (HU-B02.1). Token válido: policy Allow com o contexto
 * {@code usuarioId} e {@code papel}. Token ausente ou inválido: lança "Unauthorized", que o
 * API Gateway transforma em 401.
 */
public class AuthorizerHandler implements RequestHandler<APIGatewayCustomAuthorizerEvent, IamPolicyResponse> {

    /** Lida uma vez por container e mantida em memória. */
    private static volatile ValidadorJwt cache;

    private final Supplier<ValidadorJwt> validador;

    public AuthorizerHandler() {
        this(AuthorizerHandler::validadorDoSecret);
    }

    AuthorizerHandler(Supplier<ValidadorJwt> validador) {
        this.validador = validador;
    }

    @Override
    public IamPolicyResponse handleRequest(APIGatewayCustomAuthorizerEvent evento, Context context) {
        String header = header(evento.getHeaders(), "Authorization");
        var identidade = validador.get().validar(header)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return IamPolicyResponse.builder()
                .withPrincipalId(identidade.usuarioId())
                .withPolicyDocument(IamPolicyResponse.PolicyDocument.builder()
                        .withVersion(IamPolicyResponse.VERSION_2012_10_17)
                        .withStatement(List.of(IamPolicyResponse.allowStatement(recursoLiberado(evento.getMethodArn()))))
                        .build())
                .withContext(Map.of("usuarioId", identidade.usuarioId(), "papel", identidade.papel()))
                .build();
    }

    /** Libera a API inteira (arn:...:{api}/{stage}/*) para o resultado valer em qualquer rota. */
    static String recursoLiberado(String methodArn) {
        if (methodArn == null) {
            return "*";
        }
        String[] partes = methodArn.split("/");
        return partes.length >= 2 ? partes[0] + "/" + partes[1] + "/*" : methodArn;
    }

    static String header(Map<String, String> headers, String nome) {
        if (headers == null) {
            return null;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(nome))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static ValidadorJwt validadorDoSecret() {
        ValidadorJwt atual = cache;
        if (atual == null) {
            synchronized (AuthorizerHandler.class) {
                if (cache == null) {
                    cache = new ValidadorJwt(lerChave());
                }
                atual = cache;
            }
        }
        return atual;
    }

    private static String lerChave() {
        String direto = System.getenv("JWT_SECRET");
        if (direto != null && !direto.isBlank()) {
            return direto;
        }
        var builder = SecretsManagerClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(env("AWS_REGION", "sa-east-1")));
        String endpoint = System.getenv("AWS_ENDPOINT_URL"); // o LocalStack injeta nas Lambdas
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        try (var secrets = builder.build()) {
            return secrets.getSecretValue(r -> r.secretId(env("SECRET_JWT", "wc-jwt-secret"))).secretString();
        }
    }

    private static String env(String nome, String padrao) {
        String v = System.getenv(nome);
        return v == null || v.isBlank() ? padrao : v;
    }
}
