package br.com.wcautopecas.fiscal.xml;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Códigos IBGE de UF e de município. O cadastro do front só tem cidade e UF; a tabela cobre as
 * capitais e as cidades do seed, e cidade desconhecida cai na capital da UF (o simulador aceita;
 * emissão real exige endereço completo, seção 5.10 da arquitetura).
 */
public final class Ibge {

    private Ibge() {
    }

    static final Map<String, String> UF = Map.ofEntries(
            Map.entry("RO", "11"), Map.entry("AC", "12"), Map.entry("AM", "13"), Map.entry("RR", "14"),
            Map.entry("PA", "15"), Map.entry("AP", "16"), Map.entry("TO", "17"), Map.entry("MA", "21"),
            Map.entry("PI", "22"), Map.entry("CE", "23"), Map.entry("RN", "24"), Map.entry("PB", "25"),
            Map.entry("PE", "26"), Map.entry("AL", "27"), Map.entry("SE", "28"), Map.entry("BA", "29"),
            Map.entry("MG", "31"), Map.entry("ES", "32"), Map.entry("RJ", "33"), Map.entry("SP", "35"),
            Map.entry("PR", "41"), Map.entry("SC", "42"), Map.entry("RS", "43"), Map.entry("MS", "50"),
            Map.entry("MT", "51"), Map.entry("GO", "52"), Map.entry("DF", "53"));

    /** Capital de cada UF: {nome sem acento, código}. */
    static final Map<String, String[]> CAPITAIS = Map.ofEntries(
            Map.entry("RO", new String[] {"PORTO VELHO", "1100205"}), Map.entry("AC", new String[] {"RIO BRANCO", "1200401"}),
            Map.entry("AM", new String[] {"MANAUS", "1302603"}), Map.entry("RR", new String[] {"BOA VISTA", "1400100"}),
            Map.entry("PA", new String[] {"BELEM", "1501402"}), Map.entry("AP", new String[] {"MACAPA", "1600303"}),
            Map.entry("TO", new String[] {"PALMAS", "1721000"}), Map.entry("MA", new String[] {"SAO LUIS", "2111300"}),
            Map.entry("PI", new String[] {"TERESINA", "2211001"}), Map.entry("CE", new String[] {"FORTALEZA", "2304400"}),
            Map.entry("RN", new String[] {"NATAL", "2408102"}), Map.entry("PB", new String[] {"JOAO PESSOA", "2507507"}),
            Map.entry("PE", new String[] {"RECIFE", "2611606"}), Map.entry("AL", new String[] {"MACEIO", "2704302"}),
            Map.entry("SE", new String[] {"ARACAJU", "2800308"}), Map.entry("BA", new String[] {"SALVADOR", "2927408"}),
            Map.entry("MG", new String[] {"BELO HORIZONTE", "3106200"}), Map.entry("ES", new String[] {"VITORIA", "3205309"}),
            Map.entry("RJ", new String[] {"RIO DE JANEIRO", "3304557"}), Map.entry("SP", new String[] {"SAO PAULO", "3550308"}),
            Map.entry("PR", new String[] {"CURITIBA", "4106902"}), Map.entry("SC", new String[] {"FLORIANOPOLIS", "4205407"}),
            Map.entry("RS", new String[] {"PORTO ALEGRE", "4314902"}), Map.entry("MS", new String[] {"CAMPO GRANDE", "5002704"}),
            Map.entry("MT", new String[] {"CUIABA", "5103403"}), Map.entry("GO", new String[] {"GOIANIA", "5208707"}),
            Map.entry("DF", new String[] {"BRASILIA", "5300108"}));

    static final Map<String, String> OUTRAS = Map.of(
            "CAMPINAS/SP", "3509502", "GUARULHOS/SP", "3518800", "SANTOS/SP", "3548500",
            "SAO BERNARDO DO CAMPO/SP", "3548708", "SANTO ANDRE/SP", "3547809", "OSASCO/SP", "3534401",
            "RIBEIRAO PRETO/SP", "3543402", "SOROCABA/SP", "3552205", "NITEROI/RJ", "3303302",
            "CONTAGEM/MG", "3118601");

    public record Municipio(String codigo, String nome, String uf) {
    }

    public static String codigoUf(String uf) {
        String c = UF.get(uf == null ? "" : uf.toUpperCase(Locale.ROOT));
        if (c == null) {
            throw new IllegalArgumentException("UF desconhecida: " + uf);
        }
        return c;
    }

    public static Municipio municipio(String cidade, String uf) {
        String u = uf == null || uf.isBlank() ? "SP" : uf.toUpperCase(Locale.ROOT);
        String nome = semAcento(cidade);
        String[] capital = CAPITAIS.getOrDefault(u, CAPITAIS.get("SP"));
        if (!CAPITAIS.containsKey(u)) {
            u = "SP";
        }
        if (nome.equals(capital[0])) {
            return new Municipio(capital[1], capital[0], u);
        }
        String outra = OUTRAS.get(nome + "/" + u);
        if (outra != null) {
            return new Municipio(outra, nome, u);
        }
        return new Municipio(capital[1], capital[0], u);
    }

    public static String semAcento(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).trim();
    }
}
