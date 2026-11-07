package br.unirio.eleicao.eleicao.unit;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
import br.unirio.eleicao.eleicao.persistencia.Eleicao;
import br.unirio.eleicao.eleicao.persistencia.EleicaoRepository;
import br.unirio.eleicao.eleicao.servico.EleicaoDto;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.eleicao.servico.NovaEleicao;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleicaoServicoTest {
    private static final LocalDate DATA = LocalDate.of(2099, 10, 4);

    @Mock
    private EleicaoRepository repositorio;

    @Mock
    private AutenticacaoServico autenticacao;

    @InjectMocks
    private EleicaoServico servico;

    @Test
    void criarGuardaDadosEAutorEDevolveAgendada() {
        when(repositorio.contarPorDataETurno(DATA, 1)).thenReturn(0L);
        when(autenticacao.usuarioAtual()).thenReturn(new UsuarioAutenticado(7L, Perfil.CHEFE_SECAO));
        when(repositorio.save(any(Eleicao.class))).thenAnswer(chamada -> chamada.getArgument(0));

        EleicaoDto dto = servico.criar(novaEleicao(LocalTime.of(8, 0), LocalTime.of(17, 0)));

        ArgumentCaptor<Eleicao> salva = ArgumentCaptor.forClass(Eleicao.class);
        verify(repositorio).save(salva.capture());
        assertEquals(DATA, salva.getValue().dataEleicao());
        assertEquals(LocalTime.of(8, 0), salva.getValue().horaAbertura());
        assertEquals(LocalTime.of(17, 0), salva.getValue().horaEncerramento());
        assertEquals(7L, salva.getValue().criadaPor());
        assertEquals(StatusEleicao.AGENDADA, dto.status());
    }

    @Test
    void criarRecusaEncerramentoIgualOuAnteriorAAbertura() {
        assertThrows(RegraDeNegocioException.class,
                () -> servico.criar(novaEleicao(LocalTime.of(8, 0), LocalTime.of(8, 0))));
        assertThrows(RegraDeNegocioException.class,
                () -> servico.criar(novaEleicao(LocalTime.of(17, 0), LocalTime.of(8, 0))));
        verify(repositorio, never()).save(any());
    }

    @Test
    void criarRecusaDataETurnoRepetidos() {
        when(repositorio.contarPorDataETurno(DATA, 1)).thenReturn(1L);

        assertThrows(ConflitoException.class,
                () -> servico.criar(novaEleicao(LocalTime.of(8, 0), LocalTime.of(17, 0))));
        verify(repositorio, never()).save(any());
    }

    @Test
    void buscarDevolveSituacaoDaEleicao() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAgendada()));

        EleicaoDto dto = servico.buscar(1L);

        assertEquals(DATA, dto.data());
        assertEquals(1, dto.turno());
        assertEquals(LocalTime.of(8, 0), dto.horaAbertura());
        assertEquals(LocalTime.of(17, 0), dto.horaEncerramento());
        assertEquals(StatusEleicao.AGENDADA, dto.status());
    }

    @Test
    void buscarEleicaoInexistenteLancaNaoEncontrado() {
        when(repositorio.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.buscar(1L));
    }

    @Test
    void abrirPassaEleicaoAgendadaParaAberta() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAgendada()));

        assertEquals(StatusEleicao.ABERTA, servico.abrir(1L).status());
    }

    @Test
    void abrirRecusaEleicaoQueNaoEstaAgendada() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAberta()));

        assertThrows(RegraDeNegocioException.class, () -> servico.abrir(1L));
    }

    @Test
    void encerrarPassaEleicaoAbertaParaEncerrada() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAberta()));

        assertEquals(StatusEleicao.ENCERRADA, servico.encerrar(1L).status());
    }

    @Test
    void encerrarRecusaEleicaoQueNaoEstaAberta() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAgendada()));

        assertThrows(RegraDeNegocioException.class, () -> servico.encerrar(1L));
    }

    @Test
    void eleicaoEncerradaNaoReabre() {
        Eleicao encerrada = eleicaoAberta();
        encerrada.registrarEncerramento(DATA.atTime(17, 0));
        when(repositorio.findById(1L)).thenReturn(Optional.of(encerrada));

        assertThrows(RegraDeNegocioException.class, () -> servico.abrir(1L));
    }

    @Test
    void exigirStatusAceitaStatusEsperado() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAberta()));

        assertDoesNotThrow(() -> servico.exigirStatus(1L, StatusEleicao.ABERTA));
    }

    @Test
    void exigirStatusRecusaOutroStatus() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(eleicaoAgendada()));

        assertThrows(RegraDeNegocioException.class, () -> servico.exigirStatus(1L, StatusEleicao.ABERTA));
    }

    @Test
    void exigirStatusDeEleicaoInexistenteLancaNaoEncontrado() {
        when(repositorio.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.exigirStatus(1L, StatusEleicao.ABERTA));
    }

    private Eleicao eleicaoAberta() {
        Eleicao eleicao = eleicaoAgendada();
        eleicao.registrarAbertura(DATA.atTime(8, 0));
        return eleicao;
    }

    private Eleicao eleicaoAgendada() {
        return new Eleicao(DATA, 1, LocalTime.of(8, 0), LocalTime.of(17, 0), 7L);
    }

    private NovaEleicao novaEleicao(LocalTime abertura, LocalTime encerramento) {
        return new NovaEleicao(DATA, 1, abertura, encerramento);
    }
}
