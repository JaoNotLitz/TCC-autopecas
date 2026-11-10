package br.com.wcautopecas.fiscal.xml;

import br.com.wcautopecas.commons.validacao.Documentos;
import java.security.SecureRandom;

/**
 * Chave de 44 dígitos (HU-B08.2): cUF(2) AAMM(4) CNPJ(14) mod(2) serie(3) nNF(9) tpEmis(1) cNF(8) cDV(1).
 */
public record ChaveAcesso(String valor) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static ChaveAcesso gerar(String cUF, String aamm, String cnpj, String modelo, String serie, long nNF,
            String tpEmis) {
        String numero = String.format("%09d", nNF);
        String cNF;
        do {
            cNF = String.format("%08d", RANDOM.nextInt(100_000_000));
        } while (cNF.equals(numero.substring(1)));
        String base = cUF + aamm + cnpj + modelo + String.format("%03d", Integer.parseInt(serie)) + numero + tpEmis + cNF;
        return new ChaveAcesso(base + dv(base));
    }

    /** Módulo 11, pesos 2 a 9 da direita para a esquerda; resto 0 ou 1 dá DV 0 (mesmo cálculo do validador). */
    public static int dv(String base43) {
        return Documentos.dvChave(base43);
    }

    public String cUF() {
        return valor.substring(0, 2);
    }

    public String cNF() {
        return valor.substring(35, 43);
    }

    public String cDV() {
        return valor.substring(43);
    }

    public String modelo() {
        return valor.substring(20, 22);
    }
}
