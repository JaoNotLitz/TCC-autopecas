package br.com.wcautopecas.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.teste.InfraTeste;
import br.com.wcautopecas.reports.fiscal.NotaResumo;
import br.com.wcautopecas.reports.fiscal.NotasFiscais;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class RelatorioNotasIT extends BaseIT {

    static final NotaResumo AUTORIZADA = new NotaResumo(1, "NFE", "000123", "1",
            "35261112345678000195550010000001231123456789", "Auto Peças Silva, Filial \"Centro\"", "12345678000195",
            new BigDecimal("224.8"), "2026-11-10T21:53:38Z", "autorizada");
    static final NotaResumo CANCELADA = new NotaResumo(2, "NFCE", "000002", "1",
            "35261112345678000195650010000000021359869919", null, null, new BigDecimal("89.90"),
            "2026-11-10T22:03:28Z", "cancelada");

    @BeforeEach
    void preparar() {
        token = login(2, Papel.VENDEDOR);
    }

    @Test
    void csvComAsNotasDaListagem() {
        given(fiscal.listar("autorizada", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .willReturn(List.of(AUTORIZADA));
        var r = get("/api/notas/relatorio?status=autorizada&de=2026-11-01&ate=2026-11-30", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(r.getHeaders().getContentDisposition().isAttachment()).isTrue();
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("notas_2026-11-01_2026-11-30.csv");
        String csv = new String(r.getBody(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Tipo,Numero,Serie,Chave,Cliente,Documento,Valor,Emitida_Em,Status\r\n");
        assertThat(csv.lines().toList()).hasSize(2);
        assertThat(csv).contains("NFE,000123,1,35261112345678000195550010000001231123456789,"
                + "\"Auto Peças Silva, Filial \"\"Centro\"\"\",12345678000195,224.80,10/11/2026 18:53,autorizada");
    }

    @Test
    void soCanceladas() {
        given(fiscal.listar("cancelada", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)))
                .willReturn(List.of(CANCELADA));
        String csv = get("/api/notas/relatorio?status=cancelada&de=2026-11-01&ate=2026-11-10", String.class).getBody();
        assertThat(csv.lines().skip(1).toList()).containsExactly(
                "NFCE,000002,1,35261112345678000195650010000000021359869919,,,89.90,10/11/2026 19:03,cancelada");
    }

    @Test
    void semPeriodoUsaOMesCorrente() {
        YearMonth mes = YearMonth.now(ZoneId.of("America/Sao_Paulo"));
        given(fiscal.listar(isNull(), any(), any())).willReturn(List.of());
        var r = get("/api/notas/relatorio", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("notas_" + mes.atDay(1) + "_" + mes.atEndOfMonth() + ".csv");
        verify(fiscal).listar(null, mes.atDay(1), mes.atEndOfMonth());
        assertThat(r.getBody().lines().toList()).hasSize(1);
    }

    @Test
    void filtrosInvalidos() {
        var status = get("/api/notas/relatorio?status=pendente", JsonNode.class);
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(status.getBody().path("campos").get(0).path("campo").asText()).isEqualTo("status");
        var data = get("/api/notas/relatorio?de=10/11/2026", JsonNode.class);
        assertThat(data.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.getBody().path("message").asText()).isEqualTo("Data inválida (use AAAA-MM-DD)");
        var invertido = get("/api/notas/relatorio?de=2026-11-10&ate=2026-11-01", JsonNode.class);
        assertThat(invertido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(fiscal, never()).listar(any(), any(), any());
    }

    @Test
    void semToken() {
        assertThat(http.getForEntity("/api/notas/relatorio", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
