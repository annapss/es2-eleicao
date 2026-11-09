package br.unirio.eleicao.candidato.integration;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CandidatoIntegrationTest {
    private static final long ID_INEXISTENTE = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties propriedades;

    @Autowired
    private EntityManager entityManager;

    private String chefe;
    private Integer eleicaoId;
    private Integer partidoId;

    @BeforeEach
    void prepararEleicaoEPartido() throws Exception {
        chefe = ApoioTeste.bearer(mockMvc, propriedades.chefe().login(), propriedades.chefe().senha());
        eleicaoId = criarEleicao();
        partidoId = criarPartido("TSTA");
    }

    @Test
    void cadastroAssociaCargoEEleicaoECriaLogin() throws Exception {
        cadastrar(candidato(15, "GOVERNADOR", "cand-teste-a"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("João"))
                .andExpect(jsonPath("$.numero").value(15))
                .andExpect(jsonPath("$.partido").value("TSTA"))
                .andExpect(jsonPath("$.cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$.eleicaoId").value(eleicaoId))
                .andExpect(jsonPath("$.senha").doesNotExist());

        entityManager.flush();
        ApoioTeste.bearer(mockMvc, "cand-teste-a", "senha-teste");
    }

    @Test
    void cargoInvalidoRetorna400() throws Exception {
        cadastrar(candidato(15, "PREFEITO", "cand-teste-a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void numeroRepetidoNoMesmoCargoRetorna409() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");

        cadastrar(candidato(15, "GOVERNADOR", "cand-teste-b")).andExpect(status().isConflict());
    }

    @Test
    void mesmoNumeroEmOutroCargoEhPermitido() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");

        cadastrar(candidato(15, "SENADOR", "cand-teste-b")).andExpect(status().isCreated());
        entityManager.flush();
    }

    @Test
    void loginRepetidoRetorna409() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");

        cadastrar(candidato(45, "GOVERNADOR", "cand-teste-a")).andExpect(status().isConflict());
    }

    @Test
    void cadastroEmEleicaoQueNaoEstaAgendadaRetorna422() throws Exception {
        abrirEleicao();

        cadastrar(candidato(15, "GOVERNADOR", "cand-teste-a")).andExpect(status().isUnprocessableContent());
    }

    @Test
    void eleicaoOuPartidoInexistenteRetorna404() throws Exception {
        cadastrar(candidato(15, "GOVERNADOR", "cand-teste-a", ID_INEXISTENTE, partidoId))
                .andExpect(status().isNotFound());
        cadastrar(candidato(15, "GOVERNADOR", "cand-teste-a", eleicaoId, ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    void listagemFiltraPorEleicaoECargo() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        cadastrarCandidato(151, "SENADOR", "cand-teste-b");

        mockMvc.perform(get("/api/candidatos").header("Authorization", chefe)
                        .param("eleicaoId", eleicaoId.toString()).param("cargo", "GOVERNADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cargo").value("GOVERNADOR"));

        mockMvc.perform(get("/api/candidatos").header("Authorization", chefe)
                        .param("eleicaoId", eleicaoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filtroDeCargoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/candidatos").header("Authorization", chefe).param("cargo", "PREFEITO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void consultaPorIdLeDoBanco() throws Exception {
        Integer id = cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/candidatos/{id}", id).header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$.partido").value("TSTA"))
                .andExpect(jsonPath("$.eleicaoId").value(eleicaoId));
    }

    @Test
    void consultaDeCandidatoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/candidatos/{id}", ID_INEXISTENTE).header("Authorization", chefe))
                .andExpect(status().isNotFound());
    }

    @Test
    void alteracaoPersisteOsDados() throws Exception {
        Integer id = cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");

        atualizar(id, dadosCandidato("João Silva", 45, "SENADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Silva"));

        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/candidatos/{id}", id).header("Authorization", chefe))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.numero").value(45))
                .andExpect(jsonPath("$.cargo").value("SENADOR"));
    }

    @Test
    void alteracaoComEleicaoQueNaoEstaAgendadaRetorna422() throws Exception {
        Integer id = cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        abrirEleicao();

        atualizar(id, dadosCandidato("João Silva", 15, "GOVERNADOR")).andExpect(status().isUnprocessableContent());
    }

    @Test
    void alteracaoComNumeroDeOutroCandidatoRetorna409() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        Integer id = cadastrarCandidato(45, "GOVERNADOR", "cand-teste-b");

        atualizar(id, dadosCandidato("Maria", 15, "GOVERNADOR")).andExpect(status().isConflict());
    }

    @Test
    void exclusaoRemoveCandidatoEContaDeLogin() throws Exception {
        Integer id = cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        entityManager.flush();
        entityManager.clear();

        excluir(id).andExpect(status().isNoContent());

        entityManager.clear();
        mockMvc.perform(get("/api/candidatos/{id}", id).header("Authorization", chefe))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"cand-teste-a\",\"senha\":\"senha-teste\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exclusaoComEleicaoQueNaoEstaAgendadaRetorna422() throws Exception {
        Integer id = cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        abrirEleicao();

        excluir(id).andExpect(status().isUnprocessableContent());
    }

    @Test
    void exclusaoDeCandidatoInexistenteRetorna404() throws Exception {
        excluir(ID_INEXISTENTE).andExpect(status().isNotFound());
    }

    @Test
    void candidatoNaoAcessaGestaoDeCandidatos() throws Exception {
        cadastrarCandidato(15, "GOVERNADOR", "cand-teste-a");
        entityManager.flush();
        String candidato = ApoioTeste.bearer(mockMvc, "cand-teste-a", "senha-teste");

        mockMvc.perform(get("/api/candidatos").header("Authorization", candidato))
                .andExpect(status().isForbidden());
    }

    @Test
    void partidoComSiglaRepetidaRetorna409() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .header("Authorization", chefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sigla\":\"TSTA\",\"nome\":\"Outro\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listagemDePartidosIncluiPartidoCadastrado() throws Exception {
        mockMvc.perform(get("/api/partidos").header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(partidoId)).exists());
    }

    private void abrirEleicao() throws Exception {
        mockMvc.perform(post("/api/eleicoes/{id}/abertura", eleicaoId).header("Authorization", chefe))
                .andExpect(status().isOk());
    }

    private Integer criarEleicao() throws Exception {
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

    private Integer criarPartido(String sigla) throws Exception {
        String resposta = mockMvc.perform(post("/api/partidos")
                        .header("Authorization", chefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sigla\":\"%s\",\"nome\":\"Partido de Teste\"}".formatted(sigla)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private Integer cadastrarCandidato(int numero, String cargo, String login) throws Exception {
        String resposta = cadastrar(candidato(numero, cargo, login))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions cadastrar(String corpo) throws Exception {
        return mockMvc.perform(post("/api/candidatos")
                .header("Authorization", chefe)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions atualizar(long id, String corpo) throws Exception {
        return mockMvc.perform(put("/api/candidatos/{id}", id)
                .header("Authorization", chefe)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private ResultActions excluir(long id) throws Exception {
        return mockMvc.perform(delete("/api/candidatos/{id}", id).header("Authorization", chefe));
    }

    private String candidato(int numero, String cargo, String login) {
        return candidato(numero, cargo, login, eleicaoId, partidoId);
    }

    private String candidato(int numero, String cargo, String login, long eleicao, long partido) {
        return """
                {"nome":"João","numero":%d,"partidoId":%d,"cargo":"%s","eleicaoId":%d,
                 "login":"%s","senha":"senha-teste"}
                """.formatted(numero, partido, cargo, eleicao, login);
    }

    private String dadosCandidato(String nome, int numero, String cargo) {
        return """
                {"nome":"%s","numero":%d,"partidoId":%d,"cargo":"%s"}
                """.formatted(nome, numero, partidoId, cargo);
    }
}
