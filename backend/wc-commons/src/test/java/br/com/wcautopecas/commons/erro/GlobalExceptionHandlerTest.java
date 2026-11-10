package br.com.wcautopecas.commons.erro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.wcautopecas.commons.validacao.CpfOuCnpj;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    record Entrada(@NotBlank(message = "Nome obrigatório") String nome, @CpfOuCnpj String documento) {
    }

    @RestController
    static class Controller {
        @PostMapping("/x")
        void criar(@Valid @RequestBody Entrada e) {
        }

        @GetMapping("/erro/{tipo}")
        void erro(@PathVariable String tipo) {
            switch (tipo) {
                case "404" -> throw new NaoEncontradoException("Cliente não encontrado");
                case "409" -> throw new ConflitoException("Documento já cadastrado");
                case "422" -> throw new RegraNegocioException("Prazo de cancelamento expirado");
                case "403" -> throw new SemPermissaoException("Sem permissão");
                default -> throw new IllegalStateException("boom");
            }
        }
    }

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new Controller())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void validacaoViraBadRequestComCampos() throws Exception {
        mvc.perform(post("/x").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"documento\":\"11111111111\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("CPF ou CNPJ inválido"))
                .andExpect(jsonPath("$.campos[0].campo").value("documento"));
    }

    @Test
    void corpoMalformado() throws Exception {
        mvc.perform(post("/x").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido"));
    }

    @Test
    void excecoesDeDominio() throws Exception {
        mvc.perform(get("/erro/404")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"))
                .andExpect(jsonPath("$.erro").value("NOT_FOUND"))
                .andExpect(jsonPath("$.campos").doesNotExist());
        mvc.perform(get("/erro/409")).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        mvc.perform(get("/erro/422")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("UNPROCESSABLE_ENTITY"));
        mvc.perform(get("/erro/403")).andExpect(status().isForbidden());
    }

    @Test
    void erroInesperadoNaoVazaDetalhe() throws Exception {
        mvc.perform(get("/erro/x")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno. Tente novamente."));
    }
}
