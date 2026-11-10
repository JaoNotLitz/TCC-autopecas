package br.com.wcautopecas.fiscal.evento;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.fiscal.api.InutilizacaoDto;
import br.com.wcautopecas.fiscal.api.InutilizacaoInput;
import br.com.wcautopecas.fiscal.emissao.FiscalMensagens;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * HU-B07.6: inutilização de faixa de numeração da NF-e (modelo 55). O id vem do mesmo contador
 * dos cancelamentos ({@code eventos_fiscais}), porque os dois moram na tabela {@code fiscal_events}.
 */
@Service
public class InutilizacaoService {

    static final String MODELO = "55";
    static final Set<String> STATUS_EMITIDA = Set.of("autorizada", "cancelada", "processando");

    private final EventoRepository eventos;
    private final NotaRepository notas;
    private final IdGenerator ids;
    private final IdempotencyGuard locks;
    private final FiscalMensagens mensagens;
    private final Clock clock;

    public InutilizacaoService(EventoRepository eventos, NotaRepository notas, IdGenerator ids,
            IdempotencyGuard locks, FiscalMensagens mensagens, Clock clock) {
        this.eventos = eventos;
        this.notas = notas;
        this.ids = ids;
        this.locks = locks;
        this.mensagens = mensagens;
        this.clock = clock;
    }

    public List<InutilizacaoDto> listar() {
        return eventos.doTipo(EventoFiscal.INUTILIZACAO).stream()
                .sorted(Comparator.comparing(EventoFiscal::getRegistradoEm).reversed())
                .map(InutilizacaoDto::de)
                .toList();
    }

    public InutilizacaoDto registrar(InutilizacaoInput in, UsuarioAutenticado usuario) {
        if (in.numeroFinal() < in.numeroInicial()) {
            String msg = "O número final deve ser maior ou igual ao inicial";
            throw new ValidacaoException(msg, List.of(new CampoErro("numeroFinal", msg)));
        }
        String lock = "inutilizacao:" + MODELO + ":" + in.serie();
        if (!locks.primeiraVez(lock, Duration.ofSeconds(30))) {
            throw new ConflitoException("Outra inutilização desta série está em andamento. Tente de novo");
        }
        try {
            List<String> emitidas = notas.todas().stream()
                    .filter(n -> MODELO.equals(n.getModelo()) && in.serie().equals(n.getSerie()))
                    .filter(n -> STATUS_EMITIDA.contains(n.getStatus()))
                    .filter(n -> dentro(Long.parseLong(n.getNumero()), in.numeroInicial(), in.numeroFinal()))
                    .map(NotaItem::getNumero).sorted().toList();
            if (!emitidas.isEmpty()) {
                throw new RegraNegocioException("A faixa tem notas emitidas: " + String.join(", ", emitidas));
            }
            eventos.doTipo(EventoFiscal.INUTILIZACAO).stream()
                    .filter(e -> in.serie().equals(e.getSerie()) && !"rejeitado".equals(e.getStatus()))
                    .filter(e -> e.getNumeroInicial() <= in.numeroFinal() && in.numeroInicial() <= e.getNumeroFinal())
                    .findFirst()
                    .ifPresent(e -> {
                        throw new ConflitoException("A faixa sobrepõe a inutilização de " + e.getNumeroInicial()
                                + " a " + e.getNumeroFinal());
                    });

            EventoFiscal e = new EventoFiscal();
            e.setId(ids.proximo("eventos_fiscais"));
            e.setTipo(EventoFiscal.INUTILIZACAO);
            e.setStatus("processando");
            e.setModelo(MODELO);
            e.setSerie(in.serie());
            e.setNumeroInicial(in.numeroInicial());
            e.setNumeroFinal(in.numeroFinal());
            e.setJustificativa(in.justificativa().trim());
            e.setUsuarioId(usuario.id());
            e.setRegistradoEm(clock.instant().toString());
            eventos.salvar(e);
            mensagens.eventoFiscal(e, null);
            return InutilizacaoDto.de(e);
        } finally {
            locks.liberar(lock);
        }
    }

    private static boolean dentro(long n, long inicio, long fim) {
        return n >= inicio && n <= fim;
    }
}
