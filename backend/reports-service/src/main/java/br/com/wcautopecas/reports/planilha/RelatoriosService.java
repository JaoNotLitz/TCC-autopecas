package br.com.wcautopecas.reports.planilha;

import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.reports.fontes.Fontes;
import br.com.wcautopecas.reports.fontes.Fontes.Posicao;
import br.com.wcautopecas.reports.fontes.Fontes.Venda;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** HU-B14.2 e HU-B14.3: relatórios de vendas e de estoque em CSV ou XLSX. */
@Service
public class RelatoriosService {

    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final int MAX_DIAS = 366;
    static final Set<String> STATUS_ESTOQUE = Set.of("ok", "atencao", "baixo", "critico");

    static final List<String> COLUNAS_VENDAS = List.of("Numero", "Data", "Cliente", "Tipo_Documento",
            "Metodo_Pagamento", "Subtotal", "Desconto", "Total", "Nota_Fiscal", "Status");
    static final List<String> COLUNAS_ESTOQUE = List.of("Codigo", "Nome", "NCM", "Estoque_Atual", "Reservado",
            "Disponivel", "Estoque_Minimo", "Status", "Preco_Custo", "Preco_Venda", "Valor_Estoque");

    private final Fontes fontes;
    private final Clock clock;

    public RelatoriosService(Fontes fontes, Clock clock) {
        this.fontes = fontes;
        this.clock = clock;
    }

    /** {@code de} e {@code ate} obrigatórios, {@code de ≤ ate}, no máximo 366 dias. */
    public Planilha.Arquivo vendas(String de, String ate, String formato, Long clienteId, Long vendedorId) {
        Planilha.Formato f = Planilha.Formato.de(formato);
        LocalDate inicio = obrigatoria(de, "de");
        LocalDate fim = obrigatoria(ate, "ate");
        if (fim.isBefore(inicio)) {
            throw invalido("ate", "A data final não pode ser anterior à inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > MAX_DIAS) {
            throw invalido("ate", "O período pode ter no máximo " + MAX_DIAS + " dias");
        }
        List<Venda> vendas = fontes.vendas(inicio, fim).stream()
                .filter(v -> clienteId == null || clienteId.equals(v.clienteId()))
                .filter(v -> vendedorId == null || vendedorId.equals(v.vendedorId()))
                .toList();
        Map<Long, String> nomes = vendas.stream().anyMatch(v -> v.clienteId() != null) ? fontes.nomesClientes()
                : Map.of();

        Planilha p = new Planilha("Vendas", COLUNAS_VENDAS);
        for (Venda v : vendas) {
            p.linha(v.numero() == null ? String.valueOf(v.id()) : v.numero(), dataHora(v.data()),
                    v.clienteId() == null ? "Consumidor final" : nomes.getOrDefault(v.clienteId(), "#" + v.clienteId()),
                    v.tipoDocumento() == null ? null : v.tipoDocumento().toUpperCase(), v.metodoPagamento(),
                    v.subtotal(), v.desconto(), v.total(), v.notaFiscalId(), v.status());
        }
        return p.gerar(f, "vendas_" + inicio + "_" + fim);
    }

    /** Posição atual; {@code status} filtra por ok, atencao, baixo ou critico. */
    public Planilha.Arquivo estoque(String formato, String status) {
        Planilha.Formato f = Planilha.Formato.de(formato);
        String filtro = status == null || status.isBlank() ? null : status.trim();
        if (filtro != null && !STATUS_ESTOQUE.contains(filtro)) {
            throw invalido("status", "Use ok, atencao, baixo ou critico");
        }
        Planilha p = new Planilha("Estoque", COLUNAS_ESTOQUE);
        for (Posicao e : fontes.estoque()) {
            if (filtro != null && !filtro.equals(e.status())) {
                continue;
            }
            BigDecimal custo = e.precoCusto() == null ? BigDecimal.ZERO : e.precoCusto();
            p.linha(e.codigo(), e.nome(), e.ncm(), e.estoqueAtual(), e.reservado(), e.disponivel(),
                    e.estoqueMinimo(), e.status(), e.precoCusto(), e.precoVenda(),
                    custo.multiply(BigDecimal.valueOf(e.estoqueAtual())));
        }
        String hoje = LocalDate.now(clock.withZone(FUSO)).toString();
        return p.gerar(f, "estoque_" + (filtro == null ? "" : filtro + "_") + hoje);
    }

    static String dataHora(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return DATA_HORA.format(Instant.parse(iso).atZone(FUSO));
        } catch (DateTimeParseException e) {
            return iso;
        }
    }

    private static LocalDate obrigatoria(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw invalido(campo, "Informe a data " + (campo.equals("de") ? "inicial" : "final"));
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw invalido(campo, "Data inválida (use AAAA-MM-DD)");
        }
    }

    private static ValidacaoException invalido(String campo, String msg) {
        return new ValidacaoException(msg, List.of(new CampoErro(campo, msg)));
    }
}
