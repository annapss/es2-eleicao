package br.unirio.eleicao.votacao.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.eleitor.persistencia.Eleitor;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.votacao.persistencia.Comparecimento;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendente;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteId;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class VotoIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AutenticacaoServico autenticacao;
    @Autowired private EleicaoRepository eleicoes;
    @Autowired private PartidoRepository partidos;
    @Autowired private CandidatoRepository candidatos;

    @Autowired private EleitorRepository eleitores;
    @Autowired private VotoPendenteRepository pendentes;
    @Autowired private ComparecimentoRepository comparecimentos;
    @Autowired private EntityManager entityManager;
    private Long eleitorId;
    private String eleitor;
    private Long chefeId;
    private Eleicao eleicao;
    private Long partidoId;

    @BeforeEach
    void preparar() throws Exception {
        chefeId = autenticacao.criarConta("cs-us16", "senha-teste", Perfil.CHEFE_SECAO);
        eleitorId = autenticacao.criarConta("el-us16", "senha-teste", Perfil.ELEITOR);
        eleitores.save(new Eleitor(eleitorId, "Eleitor", "12345678901", "us17", LocalDate.of(2000, 1, 1)));
        eleitor = ApoioTeste.bearer(mockMvc, "el-us16", "senha-teste");
        eleicao = criarEleicao(1);
        partidoId = partidos.save(new Partido("US16", "Partido de teste")).id();
    }

    @Test
    void registraPendenteSemContabilizarVoto() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isCreated())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        VotoPendente voto = pendentes.findById(new VotoPendenteId(eleitorId, eleicao.id(), 4L)).orElseThrow();
        assertEquals(id, voto.getCandidatoId());
        assertNotNull(voto.getRegistradoEm());
        assertEquals(0L, comparecimentos.count());
        assertEquals(0, ((Number) entityManager.createNativeQuery("select count(*) from voto")
                .getSingleResult()).intValue());
    }

    @Test
    void segundoRegistroNaoSubstituiPrimeiraEscolha() throws Exception {
        Long primeiro = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov1");
        Long segundo = candidato(eleicao, Cargo.GOVERNADOR, 45, "gov2");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", primeiro).andExpect(status().isCreated());
        registrar(eleicao.id(), "GOVERNADOR", primeiro).andExpect(status().isConflict());
        registrar(eleicao.id(), "GOVERNADOR", segundo).andExpect(status().isConflict());
        entityManager.clear();
        assertEquals(1L, pendentes.count());
        assertEquals(primeiro, pendentes.findById(new VotoPendenteId(eleitorId, eleicao.id(), 4L))
                .orElseThrow().getCandidatoId());
    }

    @Test
    void permiteUmaEscolhaPorCargoEOutroTurno() throws Exception {
        for (Cargo cargo : Cargo.values()) {
            candidato(eleicao, cargo, 15, "cand-" + cargo);
        }
        Eleicao segundoTurno = criarEleicao(2);
        Long outro = candidato(segundoTurno, Cargo.GOVERNADOR, 15, "gov-turno2");
        eleicao.registrarAbertura(LocalDateTime.now());
        segundoTurno.registrarAbertura(LocalDateTime.now());
        for (Candidato c : candidatos.listarPorFiltro(eleicao.id(), null)) {
            registrar(eleicao.id(), c.cargo().name(), c.id()).andExpect(status().isCreated());
        }
        registrar(segundoTurno.id(), "GOVERNADOR", outro).andExpect(status().isCreated());
        assertEquals(6L, pendentes.count());
    }

    @Test
    void outroEleitorPodeVotarNoMesmoCargo() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isCreated());
        Long outroId = autenticacao.criarConta("outro", "senha-teste", Perfil.ELEITOR);
        eleitores.save(new Eleitor(outroId, "Outro", "98765432100", "outro", LocalDate.of(2000, 1, 1)));
        eleitor = ApoioTeste.bearer(mockMvc, "outro", "senha-teste");
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isCreated());
        assertEquals(2L, pendentes.count());
    }

    @Test
    void candidatoDeOutroCargoOuEleicaoRetorna422() throws Exception {
        Long mesmo = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        Long outro = candidato(criarEleicao(2), Cargo.GOVERNADOR, 45, "gov2");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "SENADOR", mesmo).andExpect(status().isUnprocessableContent());
        registrar(eleicao.id(), "GOVERNADOR", outro).andExpect(status().isUnprocessableContent());
        assertEquals(0L, pendentes.count());
    }

    @Test
    void eleicaoAgendadaOuEncerradaNaoAceitaRegistro() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isUnprocessableContent());
        eleicao.registrarAbertura(LocalDateTime.now());
        eleicao.registrarEncerramento(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isUnprocessableContent());
        assertEquals(0L, pendentes.count());
    }

    @Test
    void candidatoOuEleicaoInexistenteRetorna404() throws Exception {
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", 999999999L).andExpect(status().isNotFound());
        registrar(999999999L, "GOVERNADOR", 999999999L).andExpect(status().isNotFound());
    }

    @Test
    void votoJaConfirmadoNaoAceitaNovoRegistro() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        comparecimentos.saveAndFlush(new Comparecimento(eleitorId, eleicao.id(), 4L));
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isConflict());
        assertEquals(0L, pendentes.count());
    }

    @Test
    void corpoInvalidoRetorna400() throws Exception {
        for (String corpo : new String[]{"{}", "{\"cargo\":\"PREFEITO\",\"candidatoId\":1}",
                "{\"cargo\":\"GOVERNADOR\",\"candidatoId\":0}",
                "{\"cargo\":\"GOVERNADOR\",\"candidatoId\":-1}",
                "{\"cargo\":\"GOVERNADOR\",\"candidatoId\":null}"}) {
            mockMvc.perform(post("/api/votacao/{id}/votos", eleicao.id())
                            .header("Authorization", eleitor).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0L, pendentes.count());
    }

    @Test
    void exigeAutenticacaoPerfilECadastroEleitor() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        mockMvc.perform(post("/api/votacao/{id}/votos", eleicao.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cargo\":\"GOVERNADOR\",\"candidatoId\":%d}".formatted(id)))
                .andExpect(status().isUnauthorized());
        for (String login : new String[]{"cs-us16", "gov"}) {
            eleitor = ApoioTeste.bearer(mockMvc, login, "senha-teste");
            registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isForbidden());
        }
        autenticacao.criarConta("sem-cadastro", "senha-teste", Perfil.ELEITOR);
        eleitor = ApoioTeste.bearer(mockMvc, "sem-cadastro", "senha-teste");
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isForbidden());
        assertEquals(0L, pendentes.count());
    }

    private ResultActions registrar(Long eleicaoId, String cargo, Long candidatoId) throws Exception {
        return mockMvc.perform(post("/api/votacao/{id}/votos", eleicaoId)
                .header("Authorization", eleitor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"cargo\":\"%s\",\"candidatoId\":%d}".formatted(cargo, candidatoId)));
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
