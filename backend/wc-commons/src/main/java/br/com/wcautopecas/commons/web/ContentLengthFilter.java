package br.com.wcautopecas.commons.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Responde com {@code Content-Length} em vez de {@code Transfer-Encoding: chunked}. O API
 * Gateway do LocalStack Community (provider que executa o Lambda Authorizer) perde o corpo de
 * respostas chunked na integração HTTP_PROXY. As respostas do sistema são pequenas (JSON, XML,
 * PDF de uma nota), então guardar o corpo em memória antes de enviar não pesa.
 */
public class ContentLengthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        var wrapper = new ContentCachingResponseWrapper(resp);
        try {
            chain.doFilter(req, wrapper);
        } finally {
            wrapper.copyBodyToResponse();
        }
    }
}
