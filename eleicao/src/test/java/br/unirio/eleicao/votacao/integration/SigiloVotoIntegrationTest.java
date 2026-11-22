package br.unirio.eleicao.votacao.integration;

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
import br.unirio.eleicao.votacao.persistencia.ComparecimentoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
@Transactional
class SigiloVotoIntegrationTest {
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
    @Autowired private Environment ambiente;
    @Autowired private JdbcTemplate jdbc;
    @Autowired @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeamentos;
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
    void tabelasDeVotoEComparecimentoNaoPossuemVinculoDeIdentidade() {
        assertEquals(Set.of("id", "eleicao_id", "cargo_id", "candidato_id"), colunas("voto"));
        assertEquals(Set.of("eleitor_id", "eleicao_id", "cargo_id", "votou_em"), colunas("comparecimento"));
        var referenciasVoto = jdbc.query("PRAGMA foreign_key_list(voto)",
                (rs, linha) -> rs.getString("table"));
        assertTrue(referenciasVoto.stream().allMatch("candidato"::equals));
        var referenciasComparecimento = jdbc.query("PRAGMA foreign_key_list(comparecimento)",
                (rs, linha) -> rs.getString("table"));
        assertFalse(referenciasComparecimento.contains("voto"));
        assertFalse(referenciasComparecimento.contains("candidato"));
    }

    @Test
    void doisEleitoresConfirmamSemVinculoEntreComparecimentoEVoto() throws Exception {
        Long primeiro = candidato(eleicao, Cargo.GOVERNADOR, 15, "primeiro");
        Long segundo = candidato(eleicao, Cargo.GOVERNADOR, 45, "segundo");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", primeiro).andExpect(status().isCreated())
                .andExpect(content().string("")).andExpect(header().doesNotExist("Location"));
        confirmar().andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(header().doesNotExist("Location"));
        Long outroEleitor = autenticacao.criarConta("outro", "senha-teste", Perfil.ELEITOR);
        eleitores.save(new Eleitor(outroEleitor, "Outro", "98765432100", "outro", LocalDate.of(2000, 1, 1)));
        eleitor = ApoioTeste.bearer(mockMvc, "outro", "senha-teste");
        registrar(eleicao.id(), "GOVERNADOR", segundo).andExpect(status().isCreated())
                .andExpect(content().string(""));
        confirmar().andExpect(status().isNoContent()).andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        assertEquals(0L, pendentes.count());
        assertEquals(Set.of(eleitorId, outroEleitor), new HashSet<>(jdbc.query(
                "select eleitor_id from comparecimento", (rs, linha) -> rs.getLong(1))));
        assertEquals(Set.of(primeiro, segundo), new HashSet<>(jdbc.query(
                "select candidato_id from voto", (rs, linha) -> rs.getLong(1))));
        var ids = jdbc.queryForList("select id from voto", String.class);
        assertEquals(2, ids.size());
        assertNotEquals(ids.get(0), ids.get(1));
        assertTrue(ids.stream().allMatch(id -> id.matches("[0-9a-f]{32}")));
        assertEquals(2L, comparecimentos.count());
        assertEquals(2L, votos.count());
    }

    @Test
    void nenhumMapeamentoDeConsultaExibeVotoOuPendenteDeEleitor() {
        Set<String> consultas = new HashSet<>();
        mapeamentos.getHandlerMethods().forEach((rota, metodo) -> {
            for (String caminho : rota.getPatternValues()) {
                if (caminho.startsWith("/api/votacao")) {
                    var verbos = rota.getMethodsCondition().getMethods();
                    assertFalse(verbos.isEmpty(), "Rota sem restricao de metodo: " + caminho);
                    if (verbos.contains(RequestMethod.GET)) {
                        consultas.add(caminho);
                    }
                }
            }
        });
        assertEquals(Set.of("/api/votacao/{eleicaoId}/candidatos"), consultas);
    }

    @Test
    void nenhumPerfilConsultaVotosIndividuaisPelaApi() throws Exception {
        Long id = candidato(eleicao, Cargo.GOVERNADOR, 15, "primeiro");
        eleicao.registrarAbertura(LocalDateTime.now());
        registrar(eleicao.id(), "GOVERNADOR", id).andExpect(status().isCreated());
        confirmar().andExpect(status().isNoContent());
        for (String login : new String[]{"el-us16", "cs-us16", "primeiro"}) {
            String token = ApoioTeste.bearer(mockMvc, login, "senha-teste");
            for (String caminho : new String[]{
                    "/api/votacao/" + eleicao.id() + "/votos",
                    "/api/votacao/" + eleicao.id() + "/votos/GOVERNADOR",
                    "/api/votacao/" + eleicao.id() + "/eleitores/" + eleitorId + "/votos",
                    "/api/votacao/" + eleicao.id() + "/pendentes",
                    "/api/eleitores/" + eleitorId + "/votos"}) {
                mockMvc.perform(get(caminho).header("Authorization", token))
                        .andExpect(status().is4xxClientError());
            }
        }
    }

    @Test
    void configuracaoPadraoNaoRegistraCorposHttpOuParametrosSql() {
        assertEquals("false", ambiente.getProperty("spring.mvc.log-request-details"));
        assertEquals("false", ambiente.getProperty("spring.jpa.show-sql"));
        assertFalse(LoggerFactory.getLogger(
                "org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor")
                .isDebugEnabled());
        assertFalse(LoggerFactory.getLogger("org.hibernate.SQL").isDebugEnabled());
        assertFalse(LoggerFactory.getLogger("org.hibernate.orm.jdbc.bind").isTraceEnabled());
        assertFalse(LoggerFactory.getLogger("org.hibernate.orm.jdbc.extract").isTraceEnabled());
    }

    private Set<String> colunas(String tabela) {
        return new HashSet<>(jdbc.query("PRAGMA table_info(" + tabela + ")",
                (rs, linha) -> rs.getString("name")));
    }

    private ResultActions confirmar() throws Exception {
        return mockMvc.perform(post("/api/votacao/{id}/votos/GOVERNADOR/confirmacao", eleicao.id())
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
        return candidatos.save(new Candidato(usuarioId, login, numero, partidoId, cargo, destino.id())).id();
    }
}
