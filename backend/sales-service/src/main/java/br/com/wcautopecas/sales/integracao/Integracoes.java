package br.com.wcautopecas.sales.integracao;

import java.math.BigDecimal;

/** Chamadas síncronas a customer-service e product-service (decisão A4). */
public interface Integracoes {

    record Cliente(long id, String nome, String email, boolean ativo) {
    }

    /** Recorte do {@code GET /internal/produtos/{id}} do product-service. */
    record Produto(long id, String codigo, String nome, BigDecimal precoVenda, boolean ativo, long disponivel) {
    }

    /** Cliente pelo id; lança 404 se não existe e 503 se o serviço não responde. */
    Cliente cliente(long id);

    /** Produto com o estoque disponível; lança 404 se não existe e 503 se o serviço não responde. */
    Produto produto(long id);
}
