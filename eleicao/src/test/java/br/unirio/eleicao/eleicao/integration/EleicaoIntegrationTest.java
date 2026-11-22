package br.unirio.eleicao.eleicao.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.config.AppProperties;
import br.unirio.eleicao.shared.Perfil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class EleicaoIntegrationTest {
    private static final long ID_INEXISTENTE = 999_999_999L;
    private static final String ELEICAO = """
            {"data":"2099-10-04","turno":1,"horaAbertura":"08:00","horaEncerramento":"17:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties propriedades;

    @Autowired
    private AutenticacaoServico autenticacao;

    @Autowired
    private EntityManager entityManager;

    private String chefe;

    @BeforeEach
    void entrarComoChefe() throws Exception {
        chefe = ApoioTeste.bearer(mockMvc, propriedades.chefe().login(), propriedades.chefe().senha());
    }

    @Test
    void cadastroCriaEleicaoAgendadaAceitaPeloBanco() throws Exception {
        criar(ELEICAO, chefe)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.data").value("2099-10-04"))
                .andExpect(jsonPath("$.turno").value(1))
                .andExpect(jsonPath("$.horaAbertura").value("08:00"))
                .andExpect(jsonPath("$.horaEncerramento").value("17:00"))
                .andExpect(jsonPath("$.status").value("AGENDADA"));

        entityManager.flush();
    }

    @Test
    void camposObrigatoriosSaoValidados() throws Exception {
        criar("""
                {"turno":3,"horaAbertura":"08:00","horaEncerramento":"17:00"}
                """, chefe).andExpect(status().isBadRequest());
    }

    @Test
    void encerramentoAntesDaAberturaRetorna422() throws Exception {
        criar("""
                {"data":"2099-10-04","turno":1,"horaAbertura":"17:00","horaEncerramento":"08:00"}
                """, chefe).andExpect(status().isUnprocessableContent());
    }

    @Test
    void mesmaDataETurnoRetorna409() throws Exception {
        criar(ELEICAO, chefe).andExpect(status().isCreated());

        criar(ELEICAO, chefe).andExpect(status().isConflict());
    }

    @Test
    void eleitorNaoCadastraEleicao() throws Exception {
        criar(ELEICAO, tokenDeEleitor()).andExpect(status().isForbidden());
    }

    @Test
    void consultaRetornaSituacaoLidaDoBanco() throws Exception {
        Integer id = criarEleicao();
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/eleicoes/{id}", id).header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.data").value("2099-10-04"))
                .andExpect(jsonPath("$.turno").value(1))
                .andExpect(jsonPath("$.horaAbertura").value("08:00"))
                .andExpect(jsonPath("$.horaEncerramento").value("17:00"));
    }

    @Test
    void listagemIncluiEleicaoCadastrada() throws Exception {
        Integer id = criarEleicao();

        mockMvc.perform(get("/api/eleicoes").header("Authorization", chefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());
    }

    @Test
    void eleicaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/eleicoes/{id}", ID_INEXISTENTE).header("Authorization", chefe))
                .andExpect(status().isNotFound());
    }

    @Test
    void aberturaEEncerramentoPassamPelasRestricoesDoBanco() throws Exception {
        Integer id = criarEleicao();
        entityManager.flush();

        acao(id, "abertura", chefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"));
        entityManager.flush();

        acao(id, "encerramento", chefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/eleicoes/{id}", id).header("Authorization", chefe))
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
    }

    @Test
    void naoAbreEleicaoJaAberta() throws Exception {
        Integer id = criarEleicao();
        acao(id, "abertura", chefe).andExpect(status().isOk());

        acao(id, "abertura", chefe).andExpect(status().isUnprocessableContent());
    }

    @Test
    void naoEncerraEleicaoAgendada() throws Exception {
        Integer id = criarEleicao();

        acao(id, "encerramento", chefe).andExpect(status().isUnprocessableContent());
    }

    @Test
    void naoEncerraEleicaoJaEncerrada() throws Exception {
        Integer id = criarEleicao();
        acao(id, "abertura", chefe).andExpect(status().isOk());
        acao(id, "encerramento", chefe).andExpect(status().isOk());

        acao(id, "encerramento", chefe).andExpect(status().isUnprocessableContent());
    }

    @Test
    void abrirEEncerrarEleicaoInexistenteRetorna404() throws Exception {
        acao(ID_INEXISTENTE, "abertura", chefe).andExpect(status().isNotFound());
        acao(ID_INEXISTENTE, "encerramento", chefe).andExpect(status().isNotFound());
    }

    @Test
    void soChefeAbreEEncerra() throws Exception {
        Integer id = criarEleicao();
        String eleitor = tokenDeEleitor();

        acao(id, "abertura", eleitor).andExpect(status().isForbidden());
        acao(id, "encerramento", eleitor).andExpect(status().isForbidden());
    }

    private String tokenDeEleitor() throws Exception {
        autenticacao.criarConta("eleitor-eleicao", "senha-teste", Perfil.ELEITOR);
        return ApoioTeste.bearer(mockMvc, "eleitor-eleicao", "senha-teste");
    }

    private ResultActions acao(long id, String nome, String token) throws Exception {
        return mockMvc.perform(post("/api/eleicoes/{id}/{acao}", id, nome).header("Authorization", token));
    }

    private Integer criarEleicao() throws Exception {
        String resposta = criar(ELEICAO, chefe)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions criar(String corpo, String token) throws Exception {
        return mockMvc.perform(post("/api/eleicoes")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
