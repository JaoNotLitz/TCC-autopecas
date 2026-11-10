package br.com.wcautopecas.commons.seguranca;

import java.time.Instant;

/** Quem fez a requisição, extraído do JWT. Pode ser recebido como parâmetro de um controller. */
public record UsuarioAutenticado(long id, Papel papel, String token, String jti, Instant expiraEm) {

    public boolean admin() {
        return papel == Papel.ADMIN;
    }
}
