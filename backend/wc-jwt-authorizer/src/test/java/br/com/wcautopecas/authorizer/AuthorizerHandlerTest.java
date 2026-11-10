package br.com.wcautopecas.authorizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthorizerHandlerTest {

    static final String CHAVE =
            "dGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtMTIzNA==";
    static final String ARN = "arn:aws:execute-api:sa-east-1:000000000000:wcapi/local/GET/api/clientes";

    final AuthorizerHandler handler = new AuthorizerHandler(() -> new ValidadorJwt(CHAVE));

    static String token(String chaveBase64, Instant expira, String papel) {
        var b = Jwts.builder().subject("7").id("jti-1").issuedAt(new Date()).expiration(Date.from(expira));
        if (papel != null) {
            b.claim("papel", papel);
        }
        return b.signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(chaveBase64)), Jwts.SIG.HS512).compact();
    }

    static APIGatewayCustomAuthorizerEvent evento(String authorization) {
        var e = new APIGatewayCustomAuthorizerEvent();
        Map<String, String> h = new HashMap<>();
        if (authorization != null) {
            h.put("authorization", authorization); // minúsculo: o gateway pode repassar assim
        }
        e.setHeaders(h);
        e.setMethodArn(ARN);
        return e;
    }

    @Test
    void tokenValidoLiberaComContexto() {
        String t = token(CHAVE, Instant.now().plusSeconds(3600), "ADMIN");
        var r = handler.handleRequest(evento("Bearer " + t), null);
        assertThat(r.getPrincipalId()).isEqualTo("7");
        assertThat(r.getContext()).containsEntry("usuarioId", "7").containsEntry("papel", "ADMIN");
        @SuppressWarnings("unchecked")
        var stmt = ((Map<String, Object>[]) r.getPolicyDocument().get("Statement"))[0];
        assertThat(stmt).containsEntry("Effect", "Allow");
        assertThat((String[]) stmt.get("Resource"))
                .containsExactly("arn:aws:execute-api:sa-east-1:000000000000:wcapi/local/*");
    }

    @Test
    void semTokenNegado() {
        assertThatThrownBy(() -> handler.handleRequest(evento(null), null)).hasMessage("Unauthorized");
        assertThatThrownBy(() -> handler.handleRequest(evento("Basic abc"), null)).hasMessage("Unauthorized");
    }

    @Test
    void expiradoNegado() {
        String t = token(CHAVE, Instant.now().minusSeconds(10), "ADMIN");
        assertThatThrownBy(() -> handler.handleRequest(evento("Bearer " + t), null)).hasMessage("Unauthorized");
    }

    @Test
    void outraChaveOuAdulteradoNegado() {
        String outra = Base64.getEncoder().encodeToString(new byte[64]);
        String t = token(outra, Instant.now().plusSeconds(3600), "ADMIN");
        assertThatThrownBy(() -> handler.handleRequest(evento("Bearer " + t), null)).hasMessage("Unauthorized");

        String valido = token(CHAVE, Instant.now().plusSeconds(3600), "VENDEDOR");
        String[] p = valido.split("\\.");
        String payloadAdulterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(p[1])).replace("VENDEDOR", "ADMIN").getBytes());
        assertThatThrownBy(() -> handler.handleRequest(evento("Bearer " + p[0] + "." + payloadAdulterado + "." + p[2]),
                null)).hasMessage("Unauthorized");
    }

    @Test
    void semPapelNegado() {
        String t = token(CHAVE, Instant.now().plusSeconds(3600), null);
        assertThatThrownBy(() -> handler.handleRequest(evento("Bearer " + t), null)).hasMessage("Unauthorized");
    }
}
