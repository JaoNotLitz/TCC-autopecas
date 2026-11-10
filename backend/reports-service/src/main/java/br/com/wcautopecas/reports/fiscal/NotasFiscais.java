package br.com.wcautopecas.reports.fiscal;

import java.time.LocalDate;
import java.util.List;

/** Fonte das notas do relatório (o fiscal-service). Interface para os testes trocarem por um dublê. */
public interface NotasFiscais {

    List<NotaResumo> listar(String status, LocalDate de, LocalDate ate);
}
