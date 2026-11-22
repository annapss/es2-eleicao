package br.unirio.eleicao.votacao.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.candidato.persistencia.Candidato;
import br.unirio.eleicao.candidato.persistencia.CandidatoRepository;
import br.unirio.eleicao.candidato.persistencia.Partido;
import br.unirio.eleicao.candidato.persistencia.PartidoRepository;
import br.unirio.eleicao.eleicao.persistencia.Eleicao;
import br.unirio.eleicao.eleicao.persistencia.EleicaoRepository;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class VotacaoIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AutenticacaoServico autenticacao;
    @Autowired private EleicaoRepository eleicoes;
    @Autowired private PartidoRepository partidos;
    @Autowired private CandidatoRepository candidatos;

    private String eleitor;
    private Long chefeId;
    private Eleicao eleicao;
    private Long partidoId;

    @BeforeEach
    void preparar() throws Exception {
        chefeId = autenticacao.criarConta("cs-us16", "senha-teste", Perfil.CHEFE_SECAO);
        autenticacao.criarConta("el-us16", "senha-teste", Perfil.ELEITOR);
        eleitor = ApoioTeste.bearer(mockMvc, "el-us16", "senha-teste");
        eleicao = criarEleicao(1);
        partidoId = partidos.save(new Partido("US16", "Partido de teste")).id();
    }

    @Test
    void listaSomenteCandidatosDaEleicaoAbertaAgrupadosPorCargoEOrdenadosPorNumero() throws Exception {
        candidato(eleicao, Cargo.GOVERNADOR, 45, "gov45");
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov15");
        candidato(eleicao, Cargo.SENADOR, 151, "sen151");
        Eleicao outra = criarEleicao(2);
        candidato(outra, Cargo.PRESIDENTE, 99, "pres99");
        outra.registrarAbertura(LocalDateTime.now());
        eleicao.registrarAbertura(LocalDateTime.now());
        eleicoes.flush();

        mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()).header("Authorization", eleitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GOVERNADOR.length()").value(2))
                .andExpect(jsonPath("$.GOVERNADOR[0].id").value(id.intValue()))
                .andExpect(jsonPath("$.GOVERNADOR[0].numero").value(15))
                .andExpect(jsonPath("$.GOVERNADOR[1].numero").value(45))
                .andExpect(jsonPath("$.GOVERNADOR[0].partido").value("US16"))
                .andExpect(jsonPath("$.GOVERNADOR[0].eleicaoId").value(eleicao.id().intValue()))
                .andExpect(jsonPath("$.GOVERNADOR[0].senha").doesNotExist())
                .andExpect(jsonPath("$.SENADOR.length()").value(1))
                .andExpect(jsonPath("$.DEPUTADO_ESTADUAL").isEmpty())
                .andExpect(jsonPath("$.DEPUTADO_FEDERAL").isEmpty())
                .andExpect(jsonPath("$.PRESIDENTE").isEmpty());
    }

    @Test
    void eleicaoAbertaSemCandidatosRetornaTodosOsCargosVazios() throws Exception {
        eleicao.registrarAbertura(LocalDateTime.now());
        mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()).header("Authorization", eleitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.*[*]").isEmpty());
    }

    @Test
    void eleicaoAgendadaOuEncerradaRetorna422() throws Exception {
        mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()).header("Authorization", eleitor))
                .andExpect(status().isUnprocessableContent());
        eleicao.registrarAbertura(LocalDateTime.now());
        eleicao.registrarEncerramento(LocalDateTime.now());
        mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()).header("Authorization", eleitor))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void eleicaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/votacao/999999999/candidatos").header("Authorization", eleitor))
                .andExpect(status().isNotFound());
    }

    @Test
    void exigeAutenticacaoEPerfilEleitor() throws Exception {
        mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()))
                .andExpect(status().isUnauthorized());
        for (Perfil perfil : new Perfil[]{Perfil.CHEFE_SECAO, Perfil.CANDIDATO}) {
            String login = "restrito-" + perfil;
            autenticacao.criarConta(login, "senha-teste", perfil);
            String token = ApoioTeste.bearer(mockMvc, login, "senha-teste");
            mockMvc.perform(get("/api/votacao/{id}/candidatos", eleicao.id()).header("Authorization", token))
                    .andExpect(status().isForbidden());
        }
    }

    private Eleicao criarEleicao(int turno) {
        return eleicoes.save(new Eleicao(LocalDate.of(2099, 10, 4), turno,
                LocalTime.of(8, 0), LocalTime.of(17, 0), chefeId));
    }

    private Long candidato(Eleicao destino, Cargo cargo, int numero, String login) {
        Long usuarioId = autenticacao.criarConta(login, "senha-teste", Perfil.CANDIDATO);
        return candidatos.save(new Candidato(usuarioId, login, numero, partidoId, cargo, destino.id(), LocalDate.of(1970, 1, 1))).id();
    }
}
