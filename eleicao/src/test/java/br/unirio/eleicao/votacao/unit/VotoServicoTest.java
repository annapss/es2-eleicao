package br.unirio.eleicao.votacao.unit;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.votacao.persistencia.Voto;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendente;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteId;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoId;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import br.unirio.eleicao.votacao.servico.NovoVoto;
import br.unirio.eleicao.votacao.servico.VotoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotoServicoTest {
    @Mock private AutenticacaoServico autenticacao;
    @Mock private EleitorRepository eleitores;
    @Mock private EleicaoServico eleicoes;
    @Mock private CandidatoServico candidatos;
    @Mock private VotoPendenteRepository pendentes;
    @Mock private ComparecimentoRepository comparecimentos;
    @Mock private VotoRepository votos;
    @InjectMocks private VotoServico servico;

    private final NovoVoto dados = new NovoVoto(Cargo.GOVERNADOR, 15L);

    @BeforeEach
    void autenticar() {
        when(autenticacao.usuarioAtual()).thenReturn(new UsuarioAutenticado(9L, Perfil.ELEITOR));
    }

    @Test
    void registraEscolhaDoEleitorAutenticado() {
        prepararCandidatoValido();
        when(pendentes.inserirSeAusente(9L, 7L, 4L, 15L)).thenReturn(1);
        servico.registrar(7L, dados);
        verify(eleicoes).exigirStatus(7L, StatusEleicao.ABERTA);
        verify(pendentes).inserirSeAusente(9L, 7L, 4L, 15L);
    }

    @Test
    void duplicidadeRetornaConflito() {
        prepararCandidatoValido();
        assertThrows(ConflitoException.class, () -> servico.registrar(7L, dados));
    }

    @Test
    void votoConfirmadoNaoCriaPendente() {
        prepararCandidatoValido();
        when(comparecimentos.existsById(new ComparecimentoId(9L, 7L, 4L))).thenReturn(true);
        assertThrows(ConflitoException.class, () -> servico.registrar(7L, dados));
        verifyNoInteractions(pendentes);
    }

    @Test
    void candidatoIncompativelNaoCriaPendente() {
        when(eleitores.existsById(9L)).thenReturn(true);
        for (CandidatoDto candidato : new CandidatoDto[]{
                new CandidatoDto(15L, "Ana", 15, "TEST", Cargo.SENADOR, 7L),
                new CandidatoDto(15L, "Ana", 15, "TEST", Cargo.GOVERNADOR, 8L)}) {
            when(candidatos.buscar(15L)).thenReturn(candidato);
            assertThrows(RegraDeNegocioException.class, () -> servico.registrar(7L, dados));
        }
        verifyNoInteractions(pendentes, comparecimentos);
    }

    @Test
    void eleicaoNaoAbertaNaoConsultaCandidatos() {
        when(eleitores.existsById(9L)).thenReturn(true);
        doThrow(new RegraDeNegocioException("Eleicao nao aberta"))
                .when(eleicoes).exigirStatus(7L, StatusEleicao.ABERTA);
        assertThrows(RegraDeNegocioException.class, () -> servico.registrar(7L, dados));
        verifyNoInteractions(candidatos, pendentes, comparecimentos);
    }

    @Test
    void eleitorSemCadastroNaoRegistra() {
        assertThrows(AccessDeniedException.class, () -> servico.registrar(7L, dados));
        verifyNoInteractions(eleicoes, candidatos, pendentes, comparecimentos);
    }

    @Test
    void outroPerfilNaoRegistra() {
        when(autenticacao.usuarioAtual()).thenReturn(new UsuarioAutenticado(9L, Perfil.CANDIDATO));
        assertThrows(AccessDeniedException.class, () -> servico.registrar(7L, dados));
        verifyNoInteractions(eleitores, eleicoes, candidatos, pendentes, comparecimentos);
    }

    @Test
    void confirmarSalvaVotoSemEleitorERemovePendente() {
        VotoPendente pendente = prepararConfirmacao();
        when(comparecimentos.inserirSeAusente(9L, 7L, 4L)).thenReturn(1);
        servico.confirmar(7L, Cargo.GOVERNADOR);
        ArgumentCaptor<Voto> salvo = ArgumentCaptor.forClass(Voto.class);
        verify(votos).saveAndFlush(salvo.capture());
        assertEquals(7L, salvo.getValue().getEleicaoId());
        assertEquals(4L, salvo.getValue().getCargoId());
        assertEquals(15L, salvo.getValue().getCandidatoId());
        verify(pendentes).delete(pendente);
        verify(pendentes).flush();
    }

    @Test
    void confirmarConcorrenteNaoContabilizaDuasVezes() {
        prepararConfirmacao();
        assertThrows(ConflitoException.class, () -> servico.confirmar(7L, Cargo.GOVERNADOR));
        verifyNoInteractions(votos);
        verify(pendentes, never()).delete(any());
    }

    @Test
    void confirmarVotoJaConfirmadoRetornaConflito() {
        when(eleitores.existsById(9L)).thenReturn(true);
        when(comparecimentos.existsById(new ComparecimentoId(9L, 7L, 4L))).thenReturn(true);
        assertThrows(ConflitoException.class, () -> servico.confirmar(7L, Cargo.GOVERNADOR));
        verifyNoInteractions(votos, pendentes);
    }

    @Test
    void confirmarSemPendenteNaoCriaComparecimento() {
        when(eleitores.existsById(9L)).thenReturn(true);
        when(pendentes.findById(new VotoPendenteId(9L, 7L, 4L))).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> servico.confirmar(7L, Cargo.GOVERNADOR));
        verify(comparecimentos, never()).inserirSeAusente(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(votos);
    }

    private VotoPendente prepararConfirmacao() {
        when(eleitores.existsById(9L)).thenReturn(true);
        VotoPendente pendente = new VotoPendente(9L, 7L, 4L, 15L);
        when(pendentes.findById(new VotoPendenteId(9L, 7L, 4L))).thenReturn(Optional.of(pendente));
        return pendente;
    }

    private void prepararCandidatoValido() {
        when(eleitores.existsById(9L)).thenReturn(true);
        when(candidatos.buscar(15L)).thenReturn(
                new CandidatoDto(15L, "Ana", 15, "TEST", Cargo.GOVERNADOR, 7L));
    }
}
