package br.com.wcautopecas.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.product.estoque.EstoqueException;
import br.com.wcautopecas.product.estoque.EstoqueService;
import br.com.wcautopecas.product.estoque.Operacao;
import br.com.wcautopecas.product.produto.EstoqueItem;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class EstoqueRegrasTest {

    final EstoqueService service = new EstoqueService(null, mock(IdGenerator.class), null, null,
            new ProdutosProperties(), Clock.systemUTC());

    static EstoqueItem estoque(long atual, long reservado, long minimo) {
        EstoqueItem e = new EstoqueItem();
        e.setProdutoId(7L);
        e.setEstoqueAtual(atual);
        e.setReservado(reservado);
        e.setEstoqueMinimo(minimo);
        e.setStatus("ok");
        e.setVersao(4L);
        return e;
    }

    static Operacao op(String tipo, long q) {
        return new Operacao(tipo, 7L, q, 1L, "sale", 1L, null);
    }

    @Test
    void reservaAlemDoDisponivelRecusada() {
        assertThatThrownBy(() -> service.calcular(estoque(10, 8, 1), op("reserve", 3)))
                .isInstanceOf(EstoqueException.class).hasMessageStartingWith("ESTOQUE_INSUFICIENTE");
    }

    @Test
    void baixaMaiorQueEstoqueRecusada() {
        assertThatThrownBy(() -> service.calcular(estoque(2, 0, 1), op("decrease", 3)))
                .isInstanceOf(EstoqueException.class).hasMessageStartingWith("ESTOQUE_NEGATIVO");
    }

    @Test
    void liberacaoNuncaNegativaEVersaoAvanca() {
        var r = service.calcular(estoque(10, 2, 1), op("release", 5));
        assertThat(r.depois().getReservado()).isZero();
        assertThat(r.depois().getDisponivel()).isEqualTo(10);
        assertThat(r.depois().getVersao()).isEqualTo(5);
        assertThat(r.movimento().getQuantidade()).isEqualTo(2);
    }

    @Test
    void ajusteAbaixoDoReservadoRecusadoEOutroRecalculaStatus() {
        assertThatThrownBy(() -> service.calcular(estoque(10, 5, 1), op("adjust", 4)))
                .hasMessageStartingWith("ABAIXO_DO_RESERVADO");
        var r = service.calcular(estoque(10, 0, 10), op("adjust", 3));
        assertThat(r.depois().getStatus()).isEqualTo("critico");
        assertThat(r.movimento().getQuantidade()).isEqualTo(-7);
    }

    @Test
    void tipoDesconhecido() {
        assertThatThrownBy(() -> service.calcular(estoque(10, 0, 1), op("teleport", 1)))
                .hasMessageStartingWith("TIPO_DESCONHECIDO");
    }
}
