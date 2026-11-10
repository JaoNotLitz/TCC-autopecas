package br.com.wcautopecas.product.estoque;

/** Regra da seção 4.3: crítico ≤ 40% do mínimo; baixo < 70%; atenção < 100%; ok ≥ mínimo. */
public final class StatusEstoque {

    public static final String OK = "ok";
    public static final String ATENCAO = "atencao";
    public static final String BAIXO = "baixo";
    public static final String CRITICO = "critico";

    private StatusEstoque() {
    }

    public static String calcular(long atual, long minimo) {
        if (minimo <= 0) {
            return OK; // sem estoque mínimo definido, não há alerta
        }
        if (atual * 10 <= minimo * 4) {
            return CRITICO;
        }
        if (atual * 10 < minimo * 7) {
            return BAIXO;
        }
        if (atual < minimo) {
            return ATENCAO;
        }
        return OK;
    }

    public static boolean valido(String s) {
        return OK.equals(s) || ATENCAO.equals(s) || BAIXO.equals(s) || CRITICO.equals(s);
    }
}
