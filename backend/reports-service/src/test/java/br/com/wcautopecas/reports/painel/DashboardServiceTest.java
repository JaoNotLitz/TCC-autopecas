package br.com.wcautopecas.reports.painel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.wcautopecas.reports.RelatoriosProperties;
import br.com.wcautopecas.reports.fiscal.NotaResumo;
import br.com.wcautopecas.reports.fiscal.NotasFiscais;
import br.com.wcautopecas.reports.fontes.Fontes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/** TC-B14.1.2: "hoje" e "mês" no fuso de São Paulo, com o relógio fixo. */
class DashboardServiceTest {

    /** 11/11/2026 às 10h em São Paulo. */
    final Clock clock = Clock.fixed(Instant.parse("2026-11-11T13:00:00Z"), ZoneOffset.UTC);
    final Fontes fontes = mock(Fontes.class);
    final NotasFiscais notas = mock(NotasFiscais.class);
    final DashboardService service = new DashboardService(fontes, notas, mock(StringRedisTemplate.class),
            new ObjectMapper(), new RelatoriosProperties(), clock);

    static Fontes.Venda venda(long id, String data, String status, String total) {
        return new Fontes.Venda(id, String.format("%06d", id), data, null, 1L, "nfce", "pix", new BigDecimal(total),
                BigDecimal.ZERO, new BigDecimal(total), status, null);
    }

    static Fontes.Posicao produto(long id, String nome, long atual, String status) {
        return new Fontes.Posicao(id, "P-" + id, nome, "87083010", atual, 0, atual, 10, status,
                new BigDecimal("10"), new BigDecimal("20"));
    }

    @Test
    void vendasDoDiaNoFusoDaLojaEOrcamentosForaDaConta() {
        when(fontes.vendas(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11))).thenReturn(List.of(
                venda(1, "2026-11-11T12:00:00Z", "finalizada", "100.00"),
                venda(2, "2026-11-11T12:30:00Z", "finalizada", "150.00"),
                venda(3, "2026-11-11T12:45:00Z", "finalizada", "250.00"),
                venda(4, "2026-11-11T12:50:00Z", "orcamento", "999.00"),
                // 22h de ontem em São Paulo = 01h UTC de hoje: conta no mês, não no dia
                venda(5, "2026-11-11T01:00:00Z", "finalizada", "100.00"),
                venda(6, "2026-11-03T15:00:00Z", "cancelada", "80.00")));
        when(fontes.resumoEstoque()).thenReturn(new Fontes.ResumoEstoque(30, 6, new BigDecimal("12345.6"), 2));
        when(fontes.estoque()).thenReturn(List.of(produto(1, "Pastilha", 3, "critico"),
                produto(2, "Disco", 0, "critico"), produto(3, "Filtro", 50, "ok")));
        when(fontes.resumoClientes(any())).thenReturn(new Fontes.ResumoClientes(12, 10, 2));
        when(notas.listar(isNull(), any(), any())).thenReturn(List.of(
                new NotaResumo(1, "NFCE", "1", "1", null, null, null, new BigDecimal("100"), "2026-11-11T12:01:00Z",
                        "autorizada"),
                new NotaResumo(2, "NFCE", "2", "1", null, null, null, new BigDecimal("150"), "2026-11-11T12:31:00Z",
                        "processando"),
                new NotaResumo(3, "NFE", "3", "1", null, null, null, new BigDecimal("80"), "2026-11-03T15:01:00Z",
                        "rejeitada"),
                new NotaResumo(4, "ENTRADA", "9", "1", null, null, null, new BigDecimal("500"),
                        "2026-11-11T11:00:00Z", "autorizada")));

        Dashboard d = service.calcular();

        assertThat(d.vendas().hoje()).isEqualTo(3);
        assertThat(d.vendas().valorHoje()).isEqualByComparingTo("500.00");
        assertThat(d.vendas().mes()).isEqualTo(4);
        assertThat(d.vendas().valorMes()).isEqualByComparingTo("600.00");
        assertThat(d.vendas().ticketMedio()).isEqualByComparingTo("150.00");

        assertThat(d.estoque().totalProdutos()).isEqualTo(30);
        assertThat(d.estoque().valorEmEstoque()).isEqualByComparingTo("12345.60");
        assertThat(d.estoque().criticos()).extracting(Dashboard.Critico::nome).containsExactly("Disco", "Pastilha");

        assertThat(d.fiscal().emitidasHoje()).isEqualTo(2);
        assertThat(d.fiscal().autorizadas()).isEqualTo(1);
        assertThat(d.fiscal().processando()).isEqualTo(1);
        assertThat(d.fiscal().rejeitadas()).isEqualTo(1);
        assertThat(d.fiscal().valorHoje()).isEqualByComparingTo("100.00");

        assertThat(d.clientes().novosNoMes()).isEqualTo(2);
    }

    @Test
    void semVendasOTicketEZero() {
        when(fontes.vendas(any(), any())).thenReturn(List.of());
        when(fontes.resumoEstoque()).thenReturn(new Fontes.ResumoEstoque(0, 0, BigDecimal.ZERO, 0));
        when(fontes.estoque()).thenReturn(List.of());
        when(fontes.resumoClientes(any())).thenReturn(new Fontes.ResumoClientes(0, 0, 0));
        when(notas.listar(isNull(), any(), any())).thenReturn(List.of());

        Dashboard d = service.calcular();
        assertThat(d.vendas().ticketMedio()).isEqualByComparingTo("0.00");
        assertThat(d.vendas().hoje()).isZero();
    }
}
