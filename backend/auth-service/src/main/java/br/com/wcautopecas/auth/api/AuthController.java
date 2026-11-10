package br.com.wcautopecas.auth.api;

import br.com.wcautopecas.auth.api.Dtos.LoginRequest;
import br.com.wcautopecas.auth.api.Dtos.LoginResponse;
import br.com.wcautopecas.auth.api.Dtos.UsuarioDto;
import br.com.wcautopecas.auth.login.LoginService;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.1 da arquitetura. O gateway encaminha {@code /api/auth/*} para cá sem mudar o caminho. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService service;

    public AuthController(LoginService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req.email(), req.senha());
    }

    @GetMapping("/me")
    public UsuarioDto me(UsuarioAutenticado usuario) {
        return service.me(usuario);
    }

    /** Extra: o front atual só apaga o token local. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(UsuarioAutenticado usuario) {
        service.logout(usuario);
        return ResponseEntity.noContent().build();
    }
}
