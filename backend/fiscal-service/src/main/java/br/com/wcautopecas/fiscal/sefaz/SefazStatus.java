package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.xml.Ibge;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * HU-B09.6: na subida consulta o {@code NFeStatusServico4}; o health indicator {@code sefaz} mostra
 * o resultado sem derrubar o serviço (status {@code DEGRADED}, que não entra na agregação). Produção
 * só sobe com {@code SEFAZ_PRODUCAO_CONFIRMADO=true}.
 */
@Component("sefaz")
public class SefazStatus implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(SefazStatus.class);
    static final Status DEGRADED = new Status("DEGRADED");

    private final SefazClient sefaz;
    private final FiscalProperties props;
    private final Environment env;
    private volatile Health estado = Health.unknown().withDetail("motivo", "ainda não consultado").build();

    public SefazStatus(SefazClient sefaz, FiscalProperties props, Environment env) {
        this.sefaz = sefaz;
        this.props = props;
        this.env = env;
    }

    @PostConstruct
    void protegerProducao() {
        if ("producao".equalsIgnoreCase(props.getAmbiente())
                && !"true".equalsIgnoreCase(env.getProperty("SEFAZ_PRODUCAO_CONFIRMADO"))) {
            throw new IllegalStateException("wc.fiscal.ambiente=producao exige SEFAZ_PRODUCAO_CONFIRMADO=true "
                    + "(proteção contra emissão real acidental)");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void consultarNaSubida() {
        Thread.ofVirtual().name("sefaz-status").start(this::consultar);
    }

    public Health consultar() {
        try {
            var r = sefaz.status(Ibge.codigoUf(props.getEmitente().getUf()));
            String cStat = r.texto("cStat");
            if ("107".equals(cStat)) {
                estado = Health.up().withDetail("cStat", cStat).withDetail("ambiente", props.getAmbiente()).build();
            } else {
                log.warn("SEFAZ respondeu {} {} no status do serviço", cStat, r.texto("xMotivo"));
                estado = Health.status(DEGRADED).withDetail("cStat", String.valueOf(cStat)).build();
            }
        } catch (Exception e) {
            log.warn("SEFAZ não respondeu ao status do serviço: {}", e.getMessage());
            estado = Health.status(DEGRADED).withDetail("erro", String.valueOf(e.getMessage())).build();
        }
        return estado;
    }

    @Override
    public Health health() {
        return estado;
    }
}
