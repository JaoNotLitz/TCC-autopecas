package br.com.wcautopecas.reports.painel;

import br.com.wcautopecas.reports.RelatoriosProperties;
import br.com.wcautopecas.reports.fiscal.NotaResumo;
import br.com.wcautopecas.reports.fiscal.NotasFiscais;
import br.com.wcautopecas.reports.fontes.Fontes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * HU-B14.1: números da tela inicial montados a partir dos serviços de vendas, produtos, notas e clientes.
 * "Hoje" e "mês" no fuso de São Paulo. Fica 5 minutos no Redis ({@code dashboard:stats}); se o Redis
 * falhar, calcula na hora.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    static final String CHAVE = "dashboard:stats";
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final Fontes fontes;
    private final NotasFiscais notas;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final RelatoriosProperties props;
    private final Clock clock;

    public DashboardService(Fontes fontes, NotasFiscais notas, StringRedisTemplate redis, ObjectMapper mapper,
            RelatoriosProperties props, Clock clock) {
        this.fontes = fontes;
        this.notas = notas;
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    public Dashboard obter(boolean atualizar) {
        if (!atualizar) {
            Dashboard guardado = doCache();
            if (guardado != null) {
                return guardado;
            }
        }
        Dashboard d = calcular();
        try {
            redis.opsForValue().set(CHAVE, mapper.writeValueAsString(d), props.getCacheDashboard());
        } catch (Exception e) {
            log.warn("dashboard não foi para o cache: {}", e.getMessage());
        }
        return d;
    }

    private Dashboard doCache() {
        try {
            String json = redis.opsForValue().get(CHAVE);
            return json == null ? null : mapper.readValue(json, Dashboard.class);
        } catch (Exception e) {
            log.warn("cache do dashboard ilegível: {}", e.getMessage());
            return null;
        }
    }

    Dashboard calcular() {
        LocalDate hoje = LocalDate.now(clock.withZone(FUSO));
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        return new Dashboard(vendas(hoje, inicioMes), estoque(), fiscal(hoje, inicioMes),
                clientes(inicioMes));
    }

    private Dashboard.Vendas vendas(LocalDate hoje, LocalDate inicioMes) {
        List<Fontes.Venda> mes = fontes.vendas(inicioMes, hoje).stream()
                .filter(v -> "finalizada".equals(v.status())).toList();
        List<Fontes.Venda> doDia = mes.stream().filter(v -> hoje.equals(dia(v.data()))).toList();
        BigDecimal valorMes = soma(mes.stream().map(Fontes.Venda::total).toList());
        BigDecimal ticket = mes.isEmpty() ? dinheiro(BigDecimal.ZERO)
                : valorMes.divide(BigDecimal.valueOf(mes.size()), 2, RoundingMode.HALF_UP);
        return new Dashboard.Vendas(doDia.size(), soma(doDia.stream().map(Fontes.Venda::total).toList()),
                mes.size(), valorMes, ticket);
    }

    private Dashboard.Estoque estoque() {
        Fontes.ResumoEstoque r = fontes.resumoEstoque();
        List<Dashboard.Critico> criticos = fontes.estoque().stream()
                .filter(p -> "critico".equals(p.status()))
                .sorted(Comparator.comparingLong(Fontes.Posicao::estoqueAtual).thenComparing(Fontes.Posicao::nome))
                .limit(5)
                .map(p -> new Dashboard.Critico(p.id(), p.codigo(), p.nome(), p.ncm(), p.estoqueAtual(),
                        p.estoqueMinimo()))
                .toList();
        return new Dashboard.Estoque(r.totalProdutos(), r.estoqueBaixo(), r.itensCriticos(),
                dinheiro(r.valorEmEstoque()), criticos);
    }

    private Dashboard.Fiscal fiscal(LocalDate hoje, LocalDate inicioMes) {
        List<NotaResumo> mes = notas.listar(null, inicioMes, hoje).stream()
                .filter(n -> !"ENTRADA".equals(n.tipo())).toList();
        List<NotaResumo> doDia = mes.stream().filter(n -> hoje.equals(dia(n.emitidaEm()))).toList();
        return new Dashboard.Fiscal(doDia.size(), contar(mes, "autorizada"), contar(mes, "processando"),
                contar(mes, "rejeitada"), soma(doDia.stream().filter(n -> "autorizada".equals(n.status()))
                        .map(NotaResumo::valorTotal).toList()));
    }

    private Dashboard.Clientes clientes(LocalDate inicioMes) {
        Instant desde = inicioMes.atStartOfDay(FUSO).toInstant();
        Fontes.ResumoClientes r = fontes.resumoClientes(desde);
        return new Dashboard.Clientes(r.total(), r.ativos(), r.novos());
    }

    private static long contar(List<NotaResumo> notas, String status) {
        return notas.stream().filter(n -> status.equals(n.status())).count();
    }

    /** Dia em São Paulo de um instante ISO-8601 ({@code null} se não der para ler). */
    static LocalDate dia(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso).atZone(FUSO).toLocalDate();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal soma(List<BigDecimal> valores) {
        return dinheiro(valores.stream().filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal dinheiro(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
