package br.com.wcautopecas.commons.seguranca;

import br.com.wcautopecas.commons.erro.ApiError;
import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê o {@code Authorization: Bearer}, valida o JWT e a sessão no Redis e guarda o
 * {@link UsuarioAutenticado} na requisição. Sem token ou sem sessão: 401 no formato padrão.
 */
public class AutenticacaoFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final SessaoStore sessoes;
    private final List<String> caminhosPublicos;
    private final ObjectMapper mapper;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public AutenticacaoFilter(JwtService jwt, SessaoStore sessoes, List<String> caminhosPublicos, ObjectMapper mapper) {
        this.jwt = jwt;
        this.sessoes = sessoes;
        this.caminhosPublicos = caminhosPublicos;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return caminhosPublicos.stream().anyMatch(p -> matcher.match(p, caminho));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        UsuarioAutenticado usuario;
        try {
            usuario = autenticar(req.getHeader("Authorization"));
        } catch (NaoAutenticadoException e) {
            resp.setStatus(HttpStatus.UNAUTHORIZED.value());
            resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
            resp.setCharacterEncoding("UTF-8");
            mapper.writeValue(resp.getWriter(),
                    new ApiError(e.getMessage(), 401, HttpStatus.UNAUTHORIZED.name(), List.of()));
            return;
        }
        req.setAttribute(UsuarioAtual.ATRIBUTO, usuario);
        chain.doFilter(req, resp);
    }

    UsuarioAutenticado autenticar(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new NaoAutenticadoException(JwtService.MSG_TOKEN_INVALIDO);
        }
        UsuarioAutenticado u = jwt.validar(header.substring(7).trim());
        if (!sessoes.ativa(u.id(), u.token(), u.jti())) {
            throw new NaoAutenticadoException(JwtService.MSG_TOKEN_INVALIDO);
        }
        return u;
    }
}
