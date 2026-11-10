package br.com.wcautopecas.fiscal.nota;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import org.springframework.stereotype.Component;

/**
 * Número da nota por modelo e série (HU-B07.1 RF03), com contador atômico na tabela
 * {@code counters}: {@code NFE-55-1}, {@code NFCE-65-1}. A devolução é modelo 55 e usa a
 * sequência da NF-e. Número gerado nunca volta: falha depois dele deixa lacuna para inutilizar.
 */
@Component
public class Numerador {

    private final IdGenerator ids;

    public Numerador(IdGenerator ids) {
        this.ids = ids;
    }

    public String proximo(String modelo, String serie) {
        return String.format("%06d", ids.proximo(recurso(modelo, serie)));
    }

    /** HU-B10.2 RF04: depois de inutilizar até {@code numero}, a próxima nota sai acima dele. */
    public void pularAte(String modelo, String serie, long numero) {
        ids.garantirMinimo(recurso(modelo, serie), numero);
    }

    static String recurso(String modelo, String serie) {
        return ("65".equals(modelo) ? "NFCE" : "NFE") + "-" + modelo + "-" + serie;
    }

    public static String modelo(String tipo) {
        return "NFCE".equals(tipo) ? "65" : "55";
    }
}
