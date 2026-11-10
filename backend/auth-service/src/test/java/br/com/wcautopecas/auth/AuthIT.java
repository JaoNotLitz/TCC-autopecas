package br.com.wcautopecas.auth;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.wcautopecas.auth.usuario.Usuario;
import br.com.wcautopecas.auth.usuario.UsuarioRepository;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.RequerPapel;
import br.com.wcautopecas.commons.teste.InfraTeste;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "wc.auth.seed.habilitado=true",
        "wc.auth.seed.admin-senha=admin123",
        "wc.auth.seed.vendedor-senha=vendedor123"})
@ActiveProfiles("test")
class AuthIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
    }

    /** Endpoint só de teste para conferir o @RequerPapel do wc-commons. */
    @TestConfiguration
    static class Extra {
        @Bean
        SoAdmin soAdmin() {
            return new SoAdmin();
        }
    }

    @RestController
    static class SoAdmin {
        @DeleteMapping("/api/teste/so-admin")
        @RequerPapel(Papel.ADMIN)
        void apagar() {
        }
    }

    @Autowired
    TestRestTemplate http;
    @Autowired
    UsuarioRepository usuarios;
    @Autowired
    PasswordEncoder encoder;

    ResponseEntity<JsonNode> login(String email, String senha) {
        return http.postForEntity("/api/auth/login", Map.of("email", email, "senha", senha), JsonNode.class);
    }

    ResponseEntity<JsonNode> chamar(HttpMethod metodo, String url, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.setBearerAuth(token);
        }
        return http.exchange(url, metodo, new HttpEntity<>(h), JsonNode.class);
    }

    @Test
    void loginDevolveTokenEUsuarioDoContrato() {
        var r = login("admin@wcautopecas.com.br", "admin123");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("token").asText()).startsWith("eyJhbGciOiJIUzUxMiJ9");
        JsonNode u = r.getBody().get("usuario");
        assertThat(u.get("id").isNumber()).isTrue();
        assertThat(u.get("id").asLong()).isEqualTo(1);
        assertThat(u.get("nome").asText()).isEqualTo("Administrador");
        assertThat(u.get("email").asText()).isEqualTo("admin@wcautopecas.com.br");
        assertThat(u.get("papel").asText()).isEqualTo("ADMIN");
        assertThat(u.has("senhaHash")).isFalse();
        assertThat(usuarios.porId(1).orElseThrow().getUltimoLogin()).isNotNull();
    }

    @Test
    void emailComMaiusculasTambemEntra() {
        assertThat(login("Vendedor@WCAutopecas.com.br", "vendedor123").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void senhaErradaEEmailInexistenteTemAMesmaMensagem() {
        var a = login("vendedor@wcautopecas.com.br", "errada");
        var b = login("ninguem@wcautopecas.com.br", "qualquer");
        assertThat(a.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(b.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(a.getBody().get("message").asText()).isEqualTo("E-mail ou senha inválidos");
        assertThat(b.getBody().get("message")).isEqualTo(a.getBody().get("message"));
    }

    @Test
    void camposObrigatorios() {
        var r = http.postForEntity("/api/auth/login", Map.of("email", "", "senha", ""), JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void usuarioInativoNaoEntra() {
        criarUsuario(50, "inativo@wcautopecas.com.br", false);
        assertThat(login("inativo@wcautopecas.com.br", "senha123").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void bloqueiaNaQuintaFalhaMesmoComSenhaCerta() {
        criarUsuario(51, "bloqueio@wcautopecas.com.br", true);
        for (int i = 1; i <= 4; i++) {
            assertThat(login("bloqueio@wcautopecas.com.br", "errada").getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        var quinta = login("bloqueio@wcautopecas.com.br", "errada");
        assertThat(quinta.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(quinta.getBody().get("message").asText()).contains("Tente novamente em 15 minutos");
        assertThat(login("bloqueio@wcautopecas.com.br", "senha123").getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void sucessoZeraContadorDeFalhas() {
        criarUsuario(52, "zera@wcautopecas.com.br", true);
        for (int i = 0; i < 4; i++) {
            login("zera@wcautopecas.com.br", "errada");
        }
        assertThat(login("zera@wcautopecas.com.br", "senha123").getStatusCode()).isEqualTo(HttpStatus.OK);
        for (int i = 0; i < 4; i++) {
            assertThat(login("zera@wcautopecas.com.br", "errada").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void meComTokenSemTokenEDepoisDoLogout() {
        String token = login("vendedor@wcautopecas.com.br", "vendedor123").getBody().get("token").asText();

        var me = chamar(HttpMethod.GET, "/api/auth/me", token);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("id").asLong()).isEqualTo(2);
        assertThat(me.getBody().get("papel").asText()).isEqualTo("VENDEDOR");

        var semToken = chamar(HttpMethod.GET, "/api/auth/me", null);
        assertThat(semToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(semToken.getBody().get("status").asInt()).isEqualTo(401);
        assertThat(semToken.getBody().get("message").asText()).isNotBlank();

        assertThat(chamar(HttpMethod.POST, "/api/auth/logout", token).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.GET, "/api/auth/me", token).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void papelInsuficienteEh403() {
        String vendedor = login("vendedor@wcautopecas.com.br", "vendedor123").getBody().get("token").asText();
        String admin = login("admin@wcautopecas.com.br", "admin123").getBody().get("token").asText();
        var negado = chamar(HttpMethod.DELETE, "/api/teste/so-admin", vendedor);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(negado.getBody().get("message").asText()).isEqualTo("Você não tem permissão para esta operação");
        assertThat(chamar(HttpMethod.DELETE, "/api/teste/so-admin", admin).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthPublico() {
        assertThat(http.getForEntity("/actuator/health", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void criarUsuario(long id, String email, boolean ativo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Teste " + id);
        u.setEmail(email);
        u.setPapel("VENDEDOR");
        u.setSenhaHash(encoder.encode("senha123"));
        u.setAtivo(ativo);
        u.setCriadoEm(Instant.now().toString());
        usuarios.salvar(u);
    }
}
