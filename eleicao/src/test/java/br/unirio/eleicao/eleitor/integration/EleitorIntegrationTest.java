package br.unirio.eleicao.eleitor.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.config.AppProperties;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class EleitorIntegrationTest {
    private static final long ID_INEXISTENTE = 999_999_999L;
    private static final String CPF_A = "11111111111";
    private static final String CPF_B = "22222222222";
    private static final String TITULO_A = "111111111111";
    private static final String TITULO_B = "222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties propriedades;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private String chefe;

    @BeforeEach
    void entrarComoChefe() throws Exception {
        chefe = ApoioTeste.bearer(mockMvc, propriedades.chefe().login(), propriedades.chefe().senha());
    }

    @Test
    void cadastroSalvaDadosECredenciais() throws Exception {
        cadastrar(novoEleitor(CPF_A, TITULO_A, "eleitor-a"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.cpf").value(CPF_A))
                .andExpect(jsonPath("$.tituloEleitor").value(TITULO_A))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        entityManager.flush();
        ApoioTeste.bearer(mockMvc, "eleitor-a", "senha-teste");
    }

    @Test
    void cpfInvalidoRetorna400() throws Exception {
        cadastrar(novoEleitor("123", TITULO_A, "eleitor-a")).andExpect(status().isBadRequest());
    }

    @Test
    void cpfRepetidoRetorna409() throws Exception {
        cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");

        cadastrar(novoEleitor(CPF_A, TITULO_B, "eleitor-b")).andExpect(status().isConflict());
    }

    @Test
    void tituloRepetidoRetorna409() throws Exception {
        cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");

        cadastrar(novoEleitor(CPF_B, TITULO_A, "eleitor-b")).andExpect(status().isConflict());
    }

    @Test
    void loginRepetidoRetorna409() throws Exception {
        cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");

        cadastrar(novoEleitor(CPF_B, TITULO_B, "eleitor-a")).andExpect(status().isConflict());
    }

    @Test
    void eleitorNaoAcessaGestaoDeEleitores() throws Exception {
        cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");
        entityManager.flush();
        String eleitor = ApoioTeste.bearer(mockMvc, "eleitor-a", "senha-teste");

        mockMvc.perform(get("/api/eleitores").header("Authorization", eleitor))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultaPorIdELeDoBanco() throws Exception {
        Integer id = cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/eleitores/{id}", id).header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(CPF_A))
                .andExpect(jsonPath("$.dataNascimento").value("1990-05-20"));
    }

    @Test
    void listagemIncluiEleitorCadastrado() throws Exception {
        Integer id = cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");

        mockMvc.perform(get("/api/eleitores").header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());
    }

    @Test
    void consultaDeEleitorInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/eleitores/{id}", ID_INEXISTENTE).header("Authorization", chefe))
                .andExpect(status().isNotFound());
    }

    @Test
    void alteracaoPersisteOsDados() throws Exception {
        Integer id = cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");

        atualizar(id, dadosEleitor("Ana Maria", CPF_A, TITULO_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Maria"));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/eleitores/{id}", id).header("Authorization", chefe))
                .andExpect(jsonPath("$.nome").value("Ana Maria"));
    }

    @Test
    void alteracaoDeEleitorInexistenteRetorna404() throws Exception {
        atualizar(ID_INEXISTENTE, dadosEleitor("Ana", CPF_A, TITULO_A)).andExpect(status().isNotFound());
    }

    @Test
    void alteracaoComCpfDeOutroEleitorRetorna409() throws Exception {
        cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");
        Integer id = cadastrarEleitor(CPF_B, TITULO_B, "eleitor-b");

        atualizar(id, dadosEleitor("Bia", CPF_A, TITULO_B)).andExpect(status().isConflict());
    }

    @Test
    void exclusaoRemoveEleitorEContaDeLogin() throws Exception {
        Integer id = cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");
        entityManager.flush();
        entityManager.clear();

        excluir(id).andExpect(status().isNoContent());

        entityManager.clear();
        mockMvc.perform(get("/api/eleitores/{id}", id).header("Authorization", chefe))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"eleitor-a\",\"senha\":\"senha-teste\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exclusaoDeEleitorInexistenteRetorna404() throws Exception {
        excluir(ID_INEXISTENTE).andExpect(status().isNotFound());
    }

    @Test
    void exclusaoDeQuemJaVotouRetorna409() throws Exception {
        Integer eleitorId = cadastrarEleitor(CPF_A, TITULO_A, "eleitor-a");
        Integer eleicaoId = cadastrarEleicao();
        entityManager.flush();
        jdbc.update("INSERT INTO comparecimento (eleitor_id, eleicao_id, cargo_id) VALUES (?, ?, 1)",
                eleitorId, eleicaoId);
        entityManager.clear();

        excluir(eleitorId).andExpect(status().isConflict());
    }

    private Integer cadastrarEleicao() throws Exception {
        String resposta = mockMvc.perform(post("/api/eleicoes")
                        .header("Authorization", chefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":"2099-10-04","turno":1,"horaAbertura":"08:00","horaEncerramento":"17:00"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Integer cadastrarEleitor(String cpf, String titulo, String login) throws Exception {
        String resposta = cadastrar(novoEleitor(cpf, titulo, login))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions cadastrar(String corpo) throws Exception {
        return mockMvc.perform(post("/api/eleitores")
                .header("Authorization", chefe)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions atualizar(long id, String corpo) throws Exception {
        return mockMvc.perform(put("/api/eleitores/{id}", id)
                .header("Authorization", chefe)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions excluir(long id) throws Exception {
        return mockMvc.perform(delete("/api/eleitores/{id}", id).header("Authorization", chefe));
    }

    private String novoEleitor(String cpf, String titulo, String login) {
        return """
                {"nome":"Ana","cpf":"%s","tituloEleitor":"%s","dataNascimento":"1990-05-20",
                 "login":"%s","senha":"senha-teste"}
                """.formatted(cpf, titulo, login);
    }

    private String dadosEleitor(String nome, String cpf, String titulo) {
        return """
                {"nome":"%s","cpf":"%s","tituloEleitor":"%s","dataNascimento":"1990-05-20"}
                """.formatted(nome, cpf, titulo);
    }
}
