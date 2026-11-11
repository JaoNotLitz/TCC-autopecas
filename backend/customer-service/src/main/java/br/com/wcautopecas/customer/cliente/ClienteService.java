package br.com.wcautopecas.customer.cliente;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.CampoErro;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.validacao.Documentos;
import br.com.wcautopecas.customer.api.ClienteDto;
import br.com.wcautopecas.customer.api.ClienteInput;
import br.com.wcautopecas.customer.eventos.ClienteEventos;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private static final Collator ALFABETICA = Collator.getInstance(Locale.forLanguageTag("pt-BR"));

    static {
        ALFABETICA.setStrength(Collator.PRIMARY);
    }

    private final ClienteRepository repo;
    private final IdGenerator ids;
    private final ClienteCache cache;
    private final ClienteEventos eventos;
    private final Clock clock;

    public ClienteService(ClienteRepository repo, IdGenerator ids, ClienteCache cache, ClienteEventos eventos,
            Clock clock) {
        this.repo = repo;
        this.ids = ids;
        this.cache = cache;
        this.eventos = eventos;
        this.clock = clock;
    }

    // --- leitura -----------------------------------------------------------------------------

    /** GET /clientes: array completo, ordem alfabética, ativos e inativos. */
    public List<ClienteDto> listar(String tipo, String busca, String documento) {
        if (tipo != null && !tipo.isBlank() && !tipo.equals("PF") && !tipo.equals("PJ")) {
            throw new ValidacaoException("Tipo deve ser PF ou PJ", List.of(new CampoErro("tipo", "Use PF ou PJ")));
        }
        String t = vazioParaNull(tipo);
        String b = vazioParaNull(busca);
        String d = vazioParaNull(Documentos.digitos(documento));
        String filtros = "t=" + t + "|b=" + b + "|d=" + d;
        return cache.busca(filtros, () -> carregar(t, b, d));
    }

    private List<ClienteDto> carregar(String tipo, String busca, String documento) {
        List<ClienteItem> base;
        if (documento != null) {
            base = repo.porDocumento(documento).stream().toList();
        } else {
            base = repo.todos();
        }
        String termo = busca == null ? null : normalizar(busca);
        String termoDigitos = busca == null ? null : Documentos.digitos(busca);
        boolean soDigitos = busca != null && busca.matches("[\\d.\\-/\\s]+") && !termoDigitos.isEmpty();
        return base.stream()
                .filter(c -> tipo == null || tipo.equals(c.getTipo()))
                .filter(c -> termo == null
                        || (soDigitos ? c.getDocumento().contains(termoDigitos)
                                : contem(c.getNomeBusca(), termo) || contem(c.getEmail(), termo)
                                        || c.getDocumento().contains(termo)))
                .sorted(Comparator.comparing(ClienteItem::getNome, ALFABETICA))
                .map(ClienteDto::de)
                .toList();
    }

    public ClienteDto buscar(long id) {
        return cache.cliente(id).orElseGet(() -> {
            ClienteDto dto = ClienteDto.de(existente(id));
            cache.guardar(dto);
            return dto;
        });
    }

    // --- escrita -----------------------------------------------------------------------------

    public ClienteDto criar(ClienteInput in) {
        ClienteItem c = new ClienteItem();
        aplicar(c, in);
        verificarUnicidade(c, null);
        String agora = clock.instant().toString();
        c.setId(ids.proximo("clientes"));
        c.setTotalCompras(0L);
        c.setValorTotalCompras(0.0);
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        repo.criar(c);
        cache.invalidar(c.getId());
        eventos.publicar(ClienteEventos.CRIADO, c, null);
        return ClienteDto.de(c);
    }

    public ClienteDto atualizar(long id, ClienteInput in) {
        ClienteItem antigo = existente(id);
        ClienteItem novo = copia(antigo);
        aplicar(novo, in);
        verificarUnicidade(novo, id);
        novo.setDataAtualizacao(clock.instant().toString());
        repo.atualizar(antigo, novo);
        cache.invalidar(id);
        String emailAnterior = Objects.equals(antigo.getEmail(), novo.getEmail()) ? null
                : (antigo.getEmail() == null ? "" : antigo.getEmail());
        eventos.publicar(ClienteEventos.ATUALIZADO, novo, emailAnterior);
        return ClienteDto.de(novo);
    }

    /** Soft delete idempotente: desativar de novo não publica outro evento. */
    public void desativar(long id) {
        ClienteItem c = existente(id);
        if (!Boolean.TRUE.equals(c.getAtivo())) {
            return;
        }
        c.setAtivo(false);
        c.setStatus("inativo");
        c.setDataAtualizacao(clock.instant().toString());
        repo.salvar(c);
        cache.invalidar(id);
        eventos.publicar(ClienteEventos.DESATIVADO, c, null);
    }

    // --- regras ------------------------------------------------------------------------------

    private ClienteItem existente(long id) {
        return repo.porId(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado"));
    }

    /** Copia o input para o item, limpando máscaras e validando o que a anotação não cobre. */
    void aplicar(ClienteItem c, ClienteInput in) {
        String doc = Documentos.digitos(in.documento());
        if ("PF".equals(in.tipo())) {
            if (doc.length() != 11 || !Documentos.cpfValido(doc)) {
                throw campo("documento", doc.length() == 14 ? "CNPJ informado para pessoa física" : "CPF inválido");
            }
        } else if (doc.length() != 14 || !Documentos.cnpjValido(doc)) {
            throw campo("documento", doc.length() == 11 ? "CPF informado para pessoa jurídica" : "CNPJ inválido");
        }
        String tel = vazioParaNull(Documentos.digitos(in.telefone()));
        if (tel != null && tel.length() != 10 && tel.length() != 11) {
            throw campo("telefone", "Telefone deve ter 10 ou 11 dígitos");
        }
        c.setTipo(in.tipo());
        c.setDocumento(doc);
        c.setNome(in.nome().trim());
        c.setNomeBusca(normalizar(c.getNome()));
        String email = vazioParaNull(in.email());
        c.setEmail(email == null ? null : email.trim().toLowerCase(Locale.ROOT));
        c.setTelefone(tel);
        c.setCidade(vazioParaNull(in.cidade() == null ? null : in.cidade().trim()));
        c.setUf(vazioParaNull(in.uf()));
        String cep = vazioParaNull(Documentos.digitos(in.cep()));
        if (cep != null && cep.length() != 8) {
            throw campo("cep", "CEP deve ter 8 dígitos");
        }
        c.setCep(cep);
        c.setLogradouro(texto(in.logradouro()));
        c.setNumero(texto(in.numero()));
        c.setComplemento(texto(in.complemento()));
        c.setBairro(texto(in.bairro()));
        boolean ativo = in.ativo() == null || in.ativo();
        c.setAtivo(ativo);
        c.setStatus(ativo ? "ativo" : "inativo");
    }

    /** Checagem amigável antes da transação (que é quem garante de fato). */
    private void verificarUnicidade(ClienteItem c, Long proprioId) {
        repo.porDocumento(c.getDocumento())
                .filter(o -> !o.getId().equals(proprioId))
                .ifPresent(o -> {
                    throw new ConflitoException("Documento já cadastrado");
                });
        if (c.getEmail() != null) {
            repo.porEmail(c.getEmail())
                    .filter(o -> !o.getId().equals(proprioId))
                    .ifPresent(o -> {
                        throw new ConflitoException("E-mail já cadastrado");
                    });
        }
    }

    private static ClienteItem copia(ClienteItem o) {
        ClienteItem c = new ClienteItem();
        c.setId(o.getId());
        c.setTotalCompras(o.getTotalCompras());
        c.setValorTotalCompras(o.getValorTotalCompras());
        c.setUltimaCompra(o.getUltimaCompra());
        c.setDataCriacao(o.getDataCriacao());
        c.setDataAtualizacao(o.getDataAtualizacao());
        return c;
    }

    private static ValidacaoException campo(String campo, String msg) {
        return new ValidacaoException(msg, List.of(new CampoErro(campo, msg)));
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean contem(String campo, String termo) {
        return campo != null && campo.contains(termo);
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String texto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
