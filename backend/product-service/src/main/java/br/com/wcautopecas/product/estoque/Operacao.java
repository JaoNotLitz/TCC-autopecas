package br.com.wcautopecas.product.estoque;

/**
 * Mensagem da {@code inventory-updates-queue} (seção 6.2): {@code type} é reserve, release,
 * decrease, increase ou adjust. Em {@code adjust}, {@code quantidade} é o valor absoluto novo.
 */
public record Operacao(String type, Long produtoId, Long quantidade, Long referenciaId, String referenciaType,
        Long usuarioId, String motivo) {

    public static final String RESERVE = "reserve";
    public static final String RELEASE = "release";
    public static final String DECREASE = "decrease";
    public static final String INCREASE = "increase";
    public static final String ADJUST = "adjust";

    /** Chave de idempotência de negócio (HU-B05.6). Ajuste sem referência usa o id da mensagem. */
    public String chave(String idMensagem) {
        if (referenciaId == null) {
            return "inv:" + type + ":msg:" + idMensagem;
        }
        return "inv:" + type + ":" + referenciaType + ":" + referenciaId + ":" + produtoId;
    }
}
