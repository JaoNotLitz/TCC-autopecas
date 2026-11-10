package br.com.wcautopecas.commons.validacao;

import java.util.Set;

/** Regras de CPF, CNPJ, UF, NCM e chave de acesso. Todas esperam só dígitos. */
public final class Documentos {

    public static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
            "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private Documentos() {
    }

    public static String digitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    private static boolean soDigitos(String v, int tamanho) {
        return v != null && v.length() == tamanho && v.chars().allMatch(Character::isDigit);
    }

    private static boolean repetido(String v) {
        return v.chars().distinct().count() == 1;
    }

    public static boolean cpfValido(String cpf) {
        if (!soDigitos(cpf, 11) || repetido(cpf)) {
            return false;
        }
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += (cpf.charAt(i) - '0') * (t + 1 - i);
            }
            int dv = (soma * 10) % 11 % 10;
            if (dv != cpf.charAt(t) - '0') {
                return false;
            }
        }
        return true;
    }

    public static boolean cnpjValido(String cnpj) {
        if (!soDigitos(cnpj, 14) || repetido(cnpj)) {
            return false;
        }
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return dvCnpj(cnpj, p1) == cnpj.charAt(12) - '0' && dvCnpj(cnpj, p2) == cnpj.charAt(13) - '0';
    }

    private static int dvCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    public static boolean ufValida(String uf) {
        return uf != null && UFS.contains(uf);
    }

    public static boolean ncmValido(String ncm) {
        return soDigitos(ncm, 8);
    }

    /** 44 dígitos com o DV (módulo 11, pesos 2 a 9) no último dígito. */
    public static boolean chaveAcessoValida(String chave) {
        if (!soDigitos(chave, 44)) {
            return false;
        }
        return dvChave(chave.substring(0, 43)) == chave.charAt(43) - '0';
    }

    public static int dvChave(String chave43) {
        int soma = 0;
        int peso = 2;
        for (int i = chave43.length() - 1; i >= 0; i--) {
            soma += (chave43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }
}
