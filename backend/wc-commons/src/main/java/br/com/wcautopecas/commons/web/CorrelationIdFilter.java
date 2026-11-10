package br.com.wcautopecas.commons.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/** Usa o header {@code X-Correlation-Id} (ou gera um) e o coloca no MDC dos logs e na resposta. */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    /** Repassado pelo API Gateway a partir do contexto do authorizer. Só para log: a identidade vem do JWT. */
    public static final String HEADER_USUARIO = "X-Usuario-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);
        if (id == null || id.isBlank() || id.length() > 100) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        String usuario = req.getHeader(HEADER_USUARIO);
        if (usuario != null && usuario.matches("\\d{1,19}")) {
            MDC.put("usuarioId", usuario);
        }
        resp.setHeader(HEADER, id);
        try {
            chain.doFilter(req, resp);
        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove("usuarioId");
        }
    }
}
