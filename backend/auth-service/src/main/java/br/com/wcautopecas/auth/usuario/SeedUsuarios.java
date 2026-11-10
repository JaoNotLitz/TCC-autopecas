package br.com.wcautopecas.auth.usuario;

import br.com.wcautopecas.auth.AuthProperties;
import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.seguranca.Papel;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed do perfil local (HU-B00.4 RF01): cria o admin (id 1) e o vendedor (id 2) se ainda não
 * existirem. Fica no serviço porque a senha precisa de BCrypt.
 */
@Component
@ConditionalOnProperty(prefix = "wc.auth.seed", name = "habilitado", havingValue = "true")
public class SeedUsuarios implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUsuarios.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final IdGenerator ids;
    private final AuthProperties props;

    public SeedUsuarios(UsuarioRepository usuarios, PasswordEncoder encoder, IdGenerator ids, AuthProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.ids = ids;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        criar(1, "Administrador", "admin@wcautopecas.com.br", Papel.ADMIN, props.getSeed().getAdminSenha());
        criar(2, "Vendedor", "vendedor@wcautopecas.com.br", Papel.VENDEDOR, props.getSeed().getVendedorSenha());
        ids.garantirMinimo("usuarios", 2);
    }

    private void criar(long id, String nome, String email, Papel papel, String senha) {
        if (usuarios.porId(id).isPresent()) {
            return;
        }
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setPapel(papel.name());
        u.setSenhaHash(encoder.encode(senha));
        u.setAtivo(true);
        u.setCriadoEm(Instant.now().toString());
        usuarios.salvar(u);
        log.info("seed: usuário {} criado", email);
    }
}
