package br.com.wcautopecas.commons.seguranca;

import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import br.com.wcautopecas.commons.erro.SemPermissaoException;
import java.util.Optional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Acesso ao usuário da requisição atual (posto pelo {@link AutenticacaoFilter}). */
public final class UsuarioAtual {

    public static final String ATRIBUTO = UsuarioAutenticado.class.getName();
    public static final String MSG_SEM_PERMISSAO = "Você não tem permissão para esta operação";

    private UsuarioAtual() {
    }

    public static Optional<UsuarioAutenticado> opcional() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((UsuarioAutenticado) attrs.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST));
    }

    public static UsuarioAutenticado get() {
        return opcional().orElseThrow(() -> new NaoAutenticadoException(JwtService.MSG_TOKEN_INVALIDO));
    }

    /** Para regras condicionais (ex.: só ADMIN altera a quantidade em estoque). */
    public static void exigir(Papel... papeis) {
        UsuarioAutenticado u = get();
        for (Papel p : papeis) {
            if (u.papel() == p) {
                return;
            }
        }
        throw new SemPermissaoException(MSG_SEM_PERMISSAO);
    }
}
