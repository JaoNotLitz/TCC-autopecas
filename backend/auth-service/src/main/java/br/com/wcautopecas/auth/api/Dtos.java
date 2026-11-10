package br.com.wcautopecas.auth.api;

import br.com.wcautopecas.auth.usuario.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Contrato do front: {@code LoginResponse} e {@code Usuario} (com {@code papel} como extra). */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Informe a senha") String senha) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + "]"; // nunca loga a senha
        }
    }

    public record UsuarioDto(long id, String nome, String email, String papel) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getPapel());
        }
    }

    public record LoginResponse(String token, UsuarioDto usuario) {
    }
}
