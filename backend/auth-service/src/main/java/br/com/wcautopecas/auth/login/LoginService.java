package br.com.wcautopecas.auth.login;

import br.com.wcautopecas.auth.AuthProperties;
import br.com.wcautopecas.auth.api.Dtos.LoginResponse;
import br.com.wcautopecas.auth.api.Dtos.UsuarioDto;
import br.com.wcautopecas.auth.usuario.Usuario;
import br.com.wcautopecas.auth.usuario.UsuarioRepository;
import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.erro.NaoAutenticadoException;
import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    static final String MSG_CREDENCIAIS = "E-mail ou senha inválidos";

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SessaoStore sessoes;
    private final StringRedisTemplate redis;
    private final AuthProperties props;
    private final Clock clock;
    private final Duration validadeToken;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, SessaoStore sessoes,
            StringRedisTemplate redis, AuthProperties props, Clock clock,
            br.com.wcautopecas.commons.config.WcProperties wc) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.sessoes = sessoes;
        this.redis = redis;
        this.props = props;
        this.clock = clock;
        this.validadeToken = wc.getJwt().getValidade();
    }

    public LoginResponse login(String email, String senha) {
        Usuario u = usuarios.porEmail(email)
                .filter(x -> Boolean.TRUE.equals(x.getAtivo()))
                .orElseThrow(() -> new NaoAutenticadoException(MSG_CREDENCIAIS));

        Instant agora = clock.instant();
        verificarBloqueio(u, agora);

        if (!encoder.matches(senha, u.getSenhaHash())) {
            registrarFalha(u, agora);
            throw new NaoAutenticadoException(MSG_CREDENCIAIS);
        }

        redis.delete(chaveFalhas(u));
        u.setBloqueadoAte(null);
        u.setUltimoLogin(agora.toString());
        usuarios.salvar(u);

        var emitido = jwt.emitir(u.getId(), Papel.valueOf(u.getPapel()));
        sessoes.criar(u.getId(), emitido.token(), validadeToken);
        log.info("login do usuário {}", u.getId());
        return new LoginResponse(emitido.token(), UsuarioDto.de(u));
    }

    public UsuarioDto me(UsuarioAutenticado atual) {
        return usuarios.porId(atual.id())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .map(UsuarioDto::de)
                .orElseThrow(() -> new NaoAutenticadoException(JwtService.MSG_TOKEN_INVALIDO));
    }

    public void logout(UsuarioAutenticado atual) {
        sessoes.encerrar(atual.id(), atual.token(), atual.jti(), atual.expiraEm());
        log.info("logout do usuário {}", atual.id());
    }

    private void verificarBloqueio(Usuario u, Instant agora) {
        if (u.getBloqueadoAte() == null) {
            return;
        }
        Instant ate = Instant.parse(u.getBloqueadoAte());
        if (ate.isAfter(agora)) {
            throw bloqueado(Duration.between(agora, ate));
        }
    }

    private void registrarFalha(Usuario u, Instant agora) {
        String chave = chaveFalhas(u);
        Long falhas = redis.opsForValue().increment(chave);
        if (falhas != null && falhas == 1) {
            redis.expire(chave, props.getBloqueio());
        }
        if (falhas != null && falhas >= props.getMaxTentativas()) {
            redis.delete(chave);
            u.setBloqueadoAte(agora.plus(props.getBloqueio()).toString());
            usuarios.salvar(u);
            log.warn("usuário {} bloqueado após {} tentativas", u.getId(), falhas);
            throw bloqueado(props.getBloqueio());
        }
    }

    private static ApiException bloqueado(Duration restante) {
        long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
        return new ApiException(HttpStatus.LOCKED,
                "Usuário bloqueado temporariamente. Tente novamente em " + minutos + " minutos");
    }

    private static String chaveFalhas(Usuario u) {
        return "login-fail:" + u.getId();
    }
}
