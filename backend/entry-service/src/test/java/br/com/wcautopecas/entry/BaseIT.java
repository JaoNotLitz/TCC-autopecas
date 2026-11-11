package br.com.wcautopecas.entry;

import br.com.wcautopecas.commons.teste.InfraTeste;
import br.com.wcautopecas.entry.integracao.Integracoes;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Configuração única dos testes de integração: todos usam o mesmo contexto do Spring. Com dois contextos
 * vivos, os dois listeners da entry-events-queue disputariam as mensagens (e o mock de um deles estaria
 * sem stubs).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
    }

    @MockBean
    Integracoes integracoes;
}
