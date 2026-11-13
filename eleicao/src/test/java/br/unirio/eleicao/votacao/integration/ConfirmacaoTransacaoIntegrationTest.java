package br.unirio.eleicao.votacao.integration;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
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
import br.unirio.eleicao.votacao.persistencia.Voto;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteId;
import br.unirio.eleicao.votacao.servico.NovoVoto;
import br.unirio.eleicao.votacao.servico.VotoServico;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConfirmacaoTransacaoIntegrationTest {
    @Autowired private AutenticacaoServico autenticacao;
    @Autowired private EleicaoRepository eleicoes;
    @Autowired private EleitorRepository eleitores;
    @Autowired private CandidatoRepository candidatos;
    @Autowired private PartidoRepository partidos;
    @MockitoSpyBean private VotoPendenteRepository pendentes;
    @Autowired private ComparecimentoRepository comparecimentos;
    @Autowired private VotoServico servico;
    @MockitoSpyBean private VotoRepository votos;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void falhaNaConfirmacaoDesfazAlteracoesEPreservaPendenteParaNovaTentativa() {
        // Sem transacao de teste: o servico deve executar rollback de sua propria transacao.
        Long chefe = autenticacao.criarConta("cs-rollback", "senha", Perfil.CHEFE_SECAO);
        Long eleitor = autenticacao.criarConta("el-rollback", "senha", Perfil.ELEITOR);
        eleitores.saveAndFlush(new Eleitor(eleitor, "Eleitor", "12345678901", "rollback", LocalDate.of(2000, 1, 1)));
        Eleicao eleicao = new Eleicao(LocalDate.of(2099, 10, 4), 1,
                LocalTime.of(8, 0), LocalTime.of(17, 0), chefe);
        eleicao.registrarAbertura(LocalDateTime.now());
        eleicao = eleicoes.saveAndFlush(eleicao);
        Long partido = partidos.saveAndFlush(new Partido("RB", "Rollback")).id();
        Long usuarioCandidato = autenticacao.criarConta("ca-rollback", "senha", Perfil.CANDIDATO);
        Long candidato = candidatos.saveAndFlush(new Candidato(usuarioCandidato, "Ana", 15, partido,
                Cargo.GOVERNADOR, eleicao.id())).id();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new UsuarioAutenticado(eleitor, Perfil.ELEITOR), null, "ROLE_ELEITOR"));
        Long eleicaoId = eleicao.id();
        servico.registrar(eleicaoId, new NovoVoto(Cargo.GOVERNADOR, candidato));

        doThrow(new IllegalStateException("Falha simulada de persistencia"))
                .when(votos).saveAndFlush(any(Voto.class));
        assertThrows(IllegalStateException.class, () -> servico.confirmar(eleicaoId, Cargo.GOVERNADOR));

        assertEquals(0L, comparecimentos.count());
        assertEquals(0L, votos.count());
        assertEquals(candidato, pendentes.findById(new VotoPendenteId(eleitor, eleicaoId, 4L))
                .orElseThrow().getCandidatoId());
        reset(votos);
        // Falha depois de salvar o voto tambem deve desfazer o voto e o comparecimento.
        doThrow(new IllegalStateException("Falha simulada ao remover pendente")).when(pendentes).flush();
        assertThrows(IllegalStateException.class, () -> servico.confirmar(eleicaoId, Cargo.GOVERNADOR));
        assertEquals(0L, comparecimentos.count());
        assertEquals(0L, votos.count());
        assertEquals(1L, pendentes.count());
        reset(pendentes);
        servico.confirmar(eleicaoId, Cargo.GOVERNADOR);
        assertEquals(1L, votos.count());
        assertEquals(1L, comparecimentos.count());
        assertEquals(0L, pendentes.count());
    }
}
