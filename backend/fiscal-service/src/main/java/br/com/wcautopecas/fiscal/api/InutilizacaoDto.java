package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.fiscal.evento.EventoFiscal;

/** {@code Inutilizacao} do front (seção 5.6). */
public record InutilizacaoDto(long id, String serie, long numeroInicial, long numeroFinal, String justificativa,
        String protocolo, String registradaEm) {

    public static InutilizacaoDto de(EventoFiscal e) {
        return new InutilizacaoDto(e.getId(), e.getSerie(), e.getNumeroInicial(), e.getNumeroFinal(),
                e.getJustificativa(), e.getProtocolo(), e.getRegistradoEm());
    }
}
