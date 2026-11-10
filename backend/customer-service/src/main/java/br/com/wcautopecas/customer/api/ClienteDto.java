package br.com.wcautopecas.customer.api;

import br.com.wcautopecas.customer.cliente.ClienteItem;

/** {@code Cliente} do front. Campos de texto opcionais saem como "" (o tipo do front é string). */
public record ClienteDto(long id, String tipo, String documento, String nome, String email, String telefone,
        String cidade, String uf, boolean ativo, long totalCompras, String ultimaCompra) {

    public static ClienteDto de(ClienteItem c) {
        return new ClienteDto(c.getId(), c.getTipo(), c.getDocumento(), c.getNome(), vazio(c.getEmail()),
                vazio(c.getTelefone()), vazio(c.getCidade()), vazio(c.getUf()), Boolean.TRUE.equals(c.getAtivo()),
                c.getTotalCompras() == null ? 0 : c.getTotalCompras(), c.getUltimaCompra());
    }

    private static String vazio(String s) {
        return s == null ? "" : s;
    }
}
