package br.com.wcautopecas.commons.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Aplica {@link RequerPapel} no método ou na classe do controller. */
public class PapelInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod hm) {
            RequerPapel anotacao = AnnotatedElementUtils.findMergedAnnotation(hm.getMethod(), RequerPapel.class);
            if (anotacao == null) {
                anotacao = AnnotatedElementUtils.findMergedAnnotation(hm.getBeanType(), RequerPapel.class);
            }
            if (anotacao != null) {
                UsuarioAtual.exigir(anotacao.value());
            }
        }
        return true;
    }
}
