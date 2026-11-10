package br.com.wcautopecas.product.estoque;

/** Operação de estoque impossível (vai para a DLQ com o motivo). */
public class EstoqueException extends RuntimeException {

    private final String motivo;

    public EstoqueException(String motivo, String mensagem) {
        super(motivo + ": " + mensagem);
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }
}
