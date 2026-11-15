package br.unirio.eleicao.apuracao.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.candidato.persistencia.Candidato;
import br.unirio.eleicao.candidato.persistencia.CandidatoRepository;
import br.unirio.eleicao.candidato.persistencia.Partido;
import br.unirio.eleicao.candidato.persistencia.PartidoRepository;
import br.unirio.eleicao.eleicao.persistencia.Eleicao;
import br.unirio.eleicao.eleicao.persistencia.EleicaoRepository;
import br.unirio.eleicao.eleitor.persistencia.Eleitor;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.votacao.persistencia.Voto;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class ApuracaoIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AutenticacaoServico autenticacao;
    @Autowired private EleicaoRepository eleicoes;
    @Autowired private PartidoRepository partidos;
    @Autowired private CandidatoRepository candidatos;
    @Autowired private EleitorRepository eleitores;
    @Autowired private VotoRepository votos;
    @Autowired private EntityManager entityManager;

    private Long chefeId;
    private Long partidoId;
    private Eleicao eleicao;
    private String eleitor;

    @BeforeEach
    void preparar() throws Exception {
        chefeId = autenticacao.criarConta("cs-us21", "senha-teste", Perfil.CHEFE_SECAO);
        Long eleitorId = autenticacao.criarConta("el-us21", "senha-teste", Perfil.ELEITOR);
        eleitores.save(new Eleitor(eleitorId, "Eleitor", "12345678901", "us21", LocalDate.of(2000, 1, 1)));
        eleitor = ApoioTeste.bearer(mockMvc, "el-us21", "senha-teste");
        partidoId = partidos.save(new Partido("US21", "Partido de teste")).id();
        eleicao = criarEleicao(1);
    }

    @Test
    void eleitorAcompanhaVotosDeTodosOsCargos() throws Exception {
        Long gov1 = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov1");
        Long gov2 = candidato(eleicao, Cargo.GOVERNADOR, 25, "gov2");
        Long sen = candidato(eleicao, Cargo.SENADOR, 151, "sen");
        Long pres = candidato(eleicao, Cargo.PRESIDENTE, 10, "pres");
        eleicao.registrarAbertura(LocalDateTime.now());
        votar(eleicao, Cargo.GOVERNADOR, gov2, 2);
        votar(eleicao, Cargo.GOVERNADOR, gov1, 1);
        votar(eleicao, Cargo.PRESIDENTE, pres, 3);

        parcial(eleicao.id(), null, eleitor).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                // ordenado pela ordem do cargo e, dentro do cargo, pelos votos
                .andExpect(jsonPath("$[0].cargo").value("SENADOR"))
                .andExpect(jsonPath("$[0].candidatoId").value(sen))
                .andExpect(jsonPath("$[0].totalVotos").value(0))
                .andExpect(jsonPath("$[1].cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$[1].candidatoId").value(gov2))
                .andExpect(jsonPath("$[1].numero").value(25))
                .andExpect(jsonPath("$[1].candidato").value("gov2"))
                .andExpect(jsonPath("$[1].partido").value("US21"))
                .andExpect(jsonPath("$[1].totalVotos").value(2))
                .andExpect(jsonPath("$[2].candidatoId").value(gov1))
                .andExpect(jsonPath("$[2].totalVotos").value(1))
                .andExpect(jsonPath("$[3].cargo").value("PRESIDENTE"))
                .andExpect(jsonPath("$[3].totalVotos").value(3));
    }

    @Test
    void placarAtualizaACadaConsulta() throws Exception {
        Long gov = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());

        parcial(eleicao.id(), null, eleitor).andExpect(jsonPath("$[0].totalVotos").value(0));
        votar(eleicao, Cargo.GOVERNADOR, gov, 1);
        // Na aplicação cada requisição tem sua própria transação. Aqui o teste inteiro é uma só,
        // então limpamos o contexto para não reaproveitar a linha da view lida na consulta anterior.
        entityManager.clear();
        parcial(eleicao.id(), null, eleitor).andExpect(jsonPath("$[0].totalVotos").value(1));
    }

    @Test
    void contaSomenteVotosConfirmados() throws Exception {
        Long gov = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        mockMvc.perform(post("/api/votacao/{id}/votos", eleicao.id())
                        .header("Authorization", eleitor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cargo\":\"GOVERNADOR\",\"candidatoId\":%d}".formatted(gov)))
                .andExpect(status().isCreated());

        parcial(eleicao.id(), null, eleitor).andExpect(jsonPath("$[0].totalVotos").value(0));

        mockMvc.perform(post("/api/votacao/{id}/votos/GOVERNADOR/confirmacao", eleicao.id())
                        .header("Authorization", eleitor))
                .andExpect(status().isNoContent());
        entityManager.clear();

        parcial(eleicao.id(), null, eleitor).andExpect(jsonPath("$[0].totalVotos").value(1));
    }

    @Test
    void filtraPorCargo() throws Exception {
        candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        Long sen = candidato(eleicao, Cargo.SENADOR, 151, "sen");
        eleicao.registrarAbertura(LocalDateTime.now());

        parcial(eleicao.id(), "SENADOR", eleitor).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].candidatoId").value(sen));
    }

    @Test
    void naoMisturaCandidatosDeOutraEleicao() throws Exception {
        Eleicao outra = criarEleicao(2);
        Long gov = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        Long outroGov = candidato(outra, Cargo.GOVERNADOR, 15, "gov-t2");
        eleicao.registrarAbertura(LocalDateTime.now());
        outra.registrarAbertura(LocalDateTime.now());
        votar(outra, Cargo.GOVERNADOR, outroGov, 2);

        parcial(eleicao.id(), null, eleitor).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].candidatoId").value(gov))
                .andExpect(jsonPath("$[0].totalVotos").value(0));
    }

    @Test
    void cargoSemCandidatosRetornaListaVazia() throws Exception {
        eleicao.registrarAbertura(LocalDateTime.now());

        parcial(eleicao.id(), "PRESIDENTE", eleitor).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void chefeDaSecaoTambemAcompanha() throws Exception {
        candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        String chefe = ApoioTeste.bearer(mockMvc, "cs-us21", "senha-teste");

        parcial(eleicao.id(), null, chefe).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void eleicaoAgendadaOuEncerradaRetorna422() throws Exception {
        parcial(eleicao.id(), null, eleitor).andExpect(status().isUnprocessableContent());
        eleicao.registrarAbertura(LocalDateTime.now());
        eleicao.registrarEncerramento(LocalDateTime.now());
        parcial(eleicao.id(), null, eleitor).andExpect(status().isUnprocessableContent());
    }

    @Test
    void eleicaoInexistenteRetorna404() throws Exception {
        parcial(9999L, null, eleitor).andExpect(status().isNotFound());
    }

    @Test
    void cargoInvalidoRetorna400() throws Exception {
        eleicao.registrarAbertura(LocalDateTime.now());

        parcial(eleicao.id(), "PREFEITO", eleitor).andExpect(status().isBadRequest());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/apuracao/eleicoes/{id}/parcial", eleicao.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidatoAindaNaoAcompanha() throws Exception {
        candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        String candidato = ApoioTeste.bearer(mockMvc, "gov", "senha-teste");

        parcial(eleicao.id(), null, candidato).andExpect(status().isForbidden());
    }

    private ResultActions parcial(Long eleicaoId, String cargo, String token) throws Exception {
        var requisicao = get("/api/apuracao/eleicoes/{id}/parcial", eleicaoId).header("Authorization", token);
        if (cargo != null) {
            requisicao = requisicao.param("cargo", cargo);
        }
        return mockMvc.perform(requisicao);
    }

    private void votar(Eleicao destino, Cargo cargo, Long candidatoId, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            votos.saveAndFlush(new Voto(destino.id(), cargo.getId(), candidatoId));
        }
    }

    private Eleicao criarEleicao(int turno) {
        return eleicoes.save(new Eleicao(LocalDate.of(2099, 10, 4), turno,
                LocalTime.of(8, 0), LocalTime.of(17, 0), chefeId));
    }

    private Long candidato(Eleicao destino, Cargo cargo, int numero, String login) {
        Long usuarioId = autenticacao.criarConta(login, "senha-teste", Perfil.CANDIDATO);
        return candidatos.save(new Candidato(usuarioId, login, numero, partidoId, cargo, destino.id())).id();
    }
}
