package br.unirio.eleicao.auth.integration;

import br.unirio.eleicao.auth.repository.Usuario;
import br.unirio.eleicao.auth.repository.UsuarioRepository;
import br.unirio.eleicao.shared.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class AutenticacaoIntegrationTest {
    private static final String LOGIN = "eleitor-teste";
    private static final String SENHA = "senha-teste";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarEleitor() {
        usuarioRepository.saveAndFlush(new Usuario(LOGIN, passwordEncoder.encode(SENHA), Perfil.ELEITOR));
    }

    @Test
    void loginValidoRetornaTokenJwt() throws Exception {
        String token = obterToken();
        assertTrue(token.split("\\.").length == 3, "A resposta deve conter um JWT");
    }

    @Test
    void credenciaisInvalidasRetornam401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"eleitor-teste","senha":"senha-incorreta"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eleitorNaoAcessaRotaAdministrativa() throws Exception {
        String token = obterToken();

        mockMvc.perform(get("/api/eleicoes/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String obterToken() throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"eleitor-teste","senha":"senha-teste"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var matcher = java.util.regex.Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(resposta);
        assertTrue(matcher.find(), "A resposta de login deve conter o campo token");
        return matcher.group(1);
    }
}
