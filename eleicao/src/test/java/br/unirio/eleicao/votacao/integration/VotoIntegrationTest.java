package br.unirio.eleicao.votacao.integration;

import br.unirio.eleicao.ApoioTeste;
import br.unirio.eleicao.eleitor.persistencia.Eleitor;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    @Autowired private VotoRepository votos;
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
        assertEquals(id, voto.candidatoId());
        assertNotNull(voto.registradoEm());
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
                .orElseThrow().candidatoId());
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

    @Test
    void confirmacaoContaVotoRemovePendenteERegistraComparecimento() throws Exception {
        Long id = prepararPendente();
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.clear();
        assertEquals(0L, pendentes.count());
        assertEquals(1L, comparecimentos.count());
        var comparecimento = comparecimentos.findAll().getFirst();
        assertEquals(eleitorId, comparecimento.eleitorId());
        assertEquals(eleicao.id(), comparecimento.eleicaoId());
        assertEquals(4L, comparecimento.cargoId());
        assertNotNull(comparecimento.votouEm());
        var voto = votos.findAll().getFirst();
        assertEquals(id, voto.candidatoId());
        assertEquals(eleicao.id(), voto.eleicaoId());
        assertEquals(4L, voto.cargoId());
        assertEquals(1, totalApurado(id));
    }

    @Test
    void confirmacaoRepetidaOuNovoRegistroNaoAlteraVotoConfirmado() throws Exception {
        Long primeiro = prepararPendente();
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNoContent());
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isConflict());
        registrar(eleicao.id(), "GOVERNADOR", primeiro).andExpect(status().isConflict());
        Long outro = candidato(eleicao, Cargo.GOVERNADOR, 45, "gov-outro");
        registrar(eleicao.id(), "GOVERNADOR", outro).andExpect(status().isConflict());
        assertEquals(1L, votos.count());
        assertEquals(1, totalApurado(primeiro));
        assertEquals(0, totalApurado(outro));
        assertEquals(0L, pendentes.count());
    }

    @Test
    void confirmaSomenteOCargoEscolhido() throws Exception {
        Long governador = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        Long senador = candidato(eleicao, Cargo.SENADOR, 151, "sen");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", governador).andExpect(status().isCreated());
        registrar(eleicao.id(), "SENADOR", senador).andExpect(status().isCreated());
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNoContent());
        assertEquals(1L, pendentes.count());
        assertEquals(senador, pendentes.findAll().getFirst().candidatoId());
        assertEquals(1, totalApurado(governador));
        assertEquals(0, totalApurado(senador));
        confirmar(eleicao.id(), "SENADOR").andExpect(status().isNoContent());
        assertEquals(1, totalApurado(senador));
    }

    @Test
    void confirmacaoIsolaEleitorEEleicaoTurno() throws Exception {
        Long primeiro = prepararPendente();
        Eleicao outra = criarEleicao(2);
        Long segundo = candidato(outra, Cargo.GOVERNADOR, 45, "gov-turno2");
        outra.registrarAbertura(LocalDateTime.now());
        registrar(outra.id(), "GOVERNADOR", segundo).andExpect(status().isCreated());
        Long outroId = autenticacao.criarConta("outro", "senha-teste", Perfil.ELEITOR);
        eleitores.save(new Eleitor(outroId, "Outro", "98765432100", "outro", LocalDate.of(2000, 1, 1)));
        eleitor = ApoioTeste.bearer(mockMvc, "outro", "senha-teste");
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNotFound());
        assertEquals(2L, pendentes.count());
        registrar(eleicao.id(), "GOVERNADOR", primeiro).andExpect(status().isCreated());
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNoContent());
        assertEquals(1, totalApurado(primeiro));
        eleitor = ApoioTeste.bearer(mockMvc, "el-us16", "senha-teste");
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNoContent());
        confirmar(outra.id(), "GOVERNADOR").andExpect(status().isNoContent());
        assertEquals(2, totalApurado(primeiro));
        assertEquals(1, totalApurado(segundo));
        assertEquals(0L, pendentes.count());
    }

    @Test
    void confirmacaoSemPendenteOuEleicaoInexistenteRetorna404() throws Exception {
        eleicao.registrarAbertura(LocalDateTime.now());
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isNotFound());
        confirmar(999999999L, "GOVERNADOR").andExpect(status().isNotFound());
        assertEquals(0L, votos.count());
        assertEquals(0L, comparecimentos.count());
    }

    @Test
    void confirmacaoEmEleicaoAgendadaOuEncerradaRetorna422() throws Exception {
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isUnprocessableContent());
        prepararPendente();
        eleicao.registrarEncerramento(LocalDateTime.now());
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isUnprocessableContent());
        assertEquals(1L, pendentes.count());
        assertEquals(0L, votos.count());
        assertEquals(0L, comparecimentos.count());
    }

    @Test
    void confirmacaoExigeAutenticacaoPerfilECadastroEleitor() throws Exception {
        prepararPendente();
        mockMvc.perform(post("/api/votacao/{id}/votos/GOVERNADOR/confirmacao", eleicao.id()))
                .andExpect(status().isUnauthorized());
        for (String login : new String[]{"cs-us16", "gov"}) {
            eleitor = ApoioTeste.bearer(mockMvc, login, "senha-teste");
            confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isForbidden());
        }
        autenticacao.criarConta("sem-cadastro", "senha-teste", Perfil.ELEITOR);
        eleitor = ApoioTeste.bearer(mockMvc, "sem-cadastro", "senha-teste");
        confirmar(eleicao.id(), "GOVERNADOR").andExpect(status().isForbidden());
        assertEquals(1L, pendentes.count());
        assertEquals(0L, votos.count());
    }

    @Test
    void cargoInvalidoNaConfirmacaoRetorna400() throws Exception {
        prepararPendente();
        confirmar(eleicao.id(), "PREFEITO").andExpect(status().isBadRequest());
        assertEquals(1L, pendentes.count());
        assertEquals(0L, votos.count());
    }

    @ParameterizedTest
    @EnumSource(Cargo.class)
    void votosRejeitadosPreservamEscolhaETotalDeCadaCargo(Cargo cargo) throws Exception {
        Long primeiro = candidato(eleicao, cargo, 15, "primeiro-us19");
        Long segundo = candidato(eleicao, cargo, 45, "segundo-us19");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), cargo.name(), primeiro).andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        var chave = new VotoPendenteId(eleitorId, eleicao.id(), cargo.getId());
        var registradoEm = pendentes.findById(chave).orElseThrow().registradoEm();

        registrar(eleicao.id(), cargo.name(), primeiro).andExpect(status().isConflict());
        registrar(eleicao.id(), cargo.name(), segundo).andExpect(status().isConflict());
        entityManager.clear();
        var pendente = pendentes.findById(chave).orElseThrow();
        assertEquals(primeiro, pendente.candidatoId());
        assertEquals(registradoEm, pendente.registradoEm());
        assertEquals(1L, pendentes.count());
        assertEquals(0L, votos.count());
        assertEquals(0L, comparecimentos.count());

        confirmar(eleicao.id(), cargo.name()).andExpect(status().isNoContent());
        entityManager.clear();
        String votoId = votos.findAll().getFirst().id();
        var votouEm = comparecimentos.findAll().getFirst().votouEm();
        registrar(eleicao.id(), cargo.name(), primeiro).andExpect(status().isConflict());
        registrar(eleicao.id(), cargo.name(), segundo).andExpect(status().isConflict());
        // A imutabilidade tambem bloqueia uma tentativa com candidato inexistente.
        registrar(eleicao.id(), cargo.name(), 999999999L).andExpect(status().isConflict());
        confirmar(eleicao.id(), cargo.name()).andExpect(status().isConflict());
        entityManager.clear();
        assertEquals(0L, pendentes.count());
        assertEquals(1L, votos.count());
        assertEquals(1L, comparecimentos.count());
        assertEquals(votoId, votos.findAll().getFirst().id());
        assertEquals(primeiro, votos.findById(votoId).orElseThrow().candidatoId());
        assertEquals(votouEm, comparecimentos.findAll().getFirst().votouEm());
        assertEquals(1, totalApurado(primeiro));
        assertEquals(0, totalApurado(segundo));
    }

    private Long prepararPendente() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "gov");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isCreated());
        return id;
    }

    private int totalApurado(Long candidatoId) {
        return ((Number) entityManager.createNativeQuery(
                "select total_votos from vw_apuracao where candidato_id = :id")
                .setParameter("id", candidatoId).getSingleResult()).intValue();
    }

    private ResultActions confirmar(Long eleicaoId, String cargo) throws Exception {
        return mockMvc.perform(post("/api/votacao/{id}/votos/{cargo}/confirmacao", eleicaoId, cargo)
                .header("Authorization", eleitor));
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
        return candidatos.save(new Candidato(usuarioId, login, numero, partidoId, cargo, destino.id(), LocalDate.of(1970, 1, 1))).id();
    }
}
