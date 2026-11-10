package br.unirio.eleicao.shared.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TratadorDeErrosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppProperties propriedades;

    private String chefe;

    @BeforeEach
    void entrarComoChefe() throws Exception {
        chefe = ApoioTeste.bearer(mockMvc, propriedades.chefe().login(), propriedades.chefe().senha());
    }

    @Test
    void jsonMalformadoRetorna400ComCorpoDeErro() throws Exception {
        mockMvc.perform(post("/api/eleicoes")
                        .header("Authorization", chefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"));
    }

    @Test
    void dataEmFormatoInvalidoRetorna400ComCorpoDeErro() throws Exception {
        mockMvc.perform(post("/api/eleicoes")
                        .header("Authorization", chefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"data":"04/10/2099","turno":1,"horaAbertura":"08:00","horaEncerramento":"17:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void parametroDeTipoErradoRetorna400ComCorpoDeErro() throws Exception {
        mockMvc.perform(get("/api/eleicoes/abc").header("Authorization", chefe))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro inválido: id"));
    }
}
