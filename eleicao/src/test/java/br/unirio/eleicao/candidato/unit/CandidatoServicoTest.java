package br.unirio.eleicao.candidato.unit;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.candidato.persistencia.Candidato;
import br.unirio.eleicao.candidato.persistencia.CandidatoRepository;
import br.unirio.eleicao.candidato.persistencia.Partido;
import br.unirio.eleicao.candidato.persistencia.PartidoRepository;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.candidato.servico.DadosCandidato;
import br.unirio.eleicao.candidato.servico.NovoCandidato;
import br.unirio.eleicao.candidato.servico.NovoPartido;
import br.unirio.eleicao.candidato.servico.PartidoDto;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatoServicoTest {
    private static final Long ELEICAO = 1L;
    private static final Long PARTIDO = 3L;
    private static final Long CANDIDATO = 5L;
    private static final Long USUARIO = 9L;

    @Mock
    private CandidatoRepository candidatos;

    @Mock
    private PartidoRepository partidos;

    @Mock
    private EleicaoServico eleicao;

    @Mock
    private AutenticacaoServico autenticacao;

    @InjectMocks
    private CandidatoServico servico;

    @Test
    void cadastrarCriaContaDeCandidatoEAssociaCargoEEleicao() {
        when(partidos.existsById(PARTIDO)).thenReturn(true);
        when(autenticacao.criarConta("joao", "senha", Perfil.CANDIDATO)).thenReturn(USUARIO);
        when(candidatos.save(any(Candidato.class))).thenAnswer(chamada -> chamada.getArgument(0));

        CandidatoDto dto = servico.cadastrar(novoCandidato());

        ArgumentCaptor<Candidato> salvo = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatos).save(salvo.capture());
        assertEquals(USUARIO, salvo.getValue().usuarioId());
        assertEquals(Cargo.GOVERNADOR, dto.cargo());
        assertEquals(ELEICAO, dto.eleicaoId());
        assertEquals(15, dto.numero());
    }

    @Test
    void cadastrarExigeEleicaoAgendada() {
        doThrow(new RegraDeNegocioException("A eleição precisa estar AGENDADA"))
                .when(eleicao).exigirStatus(ELEICAO, StatusEleicao.AGENDADA);

        assertThrows(RegraDeNegocioException.class, () -> servico.cadastrar(novoCandidato()));
        verify(autenticacao, never()).criarConta(anyString(), anyString(), any());
    }

    @Test
    void cadastrarRecusaPartidoInexistente() {
        when(partidos.existsById(PARTIDO)).thenReturn(false);

        assertThrows(NaoEncontradoException.class, () -> servico.cadastrar(novoCandidato()));
        verify(autenticacao, never()).criarConta(anyString(), anyString(), any());
    }

    @Test
    void cadastrarRecusaNumeroRepetidoNoCargoENaEleicao() {
        when(partidos.existsById(PARTIDO)).thenReturn(true);
        when(candidatos.contarPorNumero(ELEICAO, Cargo.GOVERNADOR, 15)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.cadastrar(novoCandidato()));
        verify(autenticacao, never()).criarConta(anyString(), anyString(), any());
    }

    @Test
    void cadastrarConverteLoginRepetidoEmConflito() {
        when(partidos.existsById(PARTIDO)).thenReturn(true);
        when(autenticacao.criarConta("joao", "senha", Perfil.CANDIDATO))
                .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed"));

        assertThrows(ConflitoException.class, () -> servico.cadastrar(novoCandidato()));
    }

    @Test
    void listarRepassaFiltrosEMostraSiglaDoPartido() {
        Partido partido = mock(Partido.class);
        when(partido.id()).thenReturn(PARTIDO);
        when(partido.sigla()).thenReturn("PXT");
        when(partidos.findAll()).thenReturn(List.of(partido));
        when(candidatos.listarPorFiltro(ELEICAO, Cargo.GOVERNADOR)).thenReturn(List.of(candidato()));

        List<CandidatoDto> lista = servico.listar(ELEICAO, Cargo.GOVERNADOR);

        assertEquals(1, lista.size());
        assertEquals("PXT", lista.get(0).partido());
    }

    @Test
    void buscarCandidatoInexistenteLancaNaoEncontrado() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.buscar(CANDIDATO));
    }

    @Test
    void buscarPorUsuarioDevolveACandidaturaDaEleicao() {
        when(candidatos.buscarPorUsuarioEEleicao(USUARIO, ELEICAO)).thenReturn(Optional.of(candidato()));

        Optional<CandidatoDto> encontrado = servico.buscarPorUsuario(USUARIO, ELEICAO);

        assertTrue(encontrado.isPresent());
        assertEquals(Cargo.GOVERNADOR, encontrado.get().cargo());
    }

    @Test
    void atualizarAlteraOsDados() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato()));
        when(partidos.existsById(PARTIDO)).thenReturn(true);

        CandidatoDto dto = servico.atualizar(CANDIDATO, new DadosCandidato("João Silva", 45, PARTIDO, Cargo.SENADOR));

        assertEquals("João Silva", dto.nome());
        assertEquals(45, dto.numero());
        assertEquals(Cargo.SENADOR, dto.cargo());
    }

    @Test
    void atualizarExigeEleicaoAgendada() {
        Candidato candidato = candidato();
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato));
        doThrow(new RegraDeNegocioException("A eleição precisa estar AGENDADA"))
                .when(eleicao).exigirStatus(ELEICAO, StatusEleicao.AGENDADA);

        assertThrows(RegraDeNegocioException.class, () -> servico.atualizar(CANDIDATO, dadosCandidato("Outro")));
        assertEquals("João", candidato.nome());
    }

    @Test
    void atualizarRecusaNumeroDeOutroCandidato() {
        Candidato candidato = candidato();
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato));
        when(partidos.existsById(PARTIDO)).thenReturn(true);
        when(candidatos.contarPorNumeroDeOutroCandidato(ELEICAO, Cargo.GOVERNADOR, 15, CANDIDATO)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.atualizar(CANDIDATO, dadosCandidato("Outro")));
        assertEquals("João", candidato.nome());
    }

    @Test
    void atualizarCandidatoInexistenteLancaNaoEncontrado() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.atualizar(CANDIDATO, dadosCandidato("Outro")));
    }

    @Test
    void excluirRemoveAContaEForcaAEscritaNoBanco() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato()));

        servico.excluir(CANDIDATO);

        verify(autenticacao).removerConta(USUARIO);
        verify(candidatos).flush();
    }

    @Test
    void excluirExigeEleicaoAgendada() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato()));
        doThrow(new RegraDeNegocioException("A eleição precisa estar AGENDADA"))
                .when(eleicao).exigirStatus(ELEICAO, StatusEleicao.AGENDADA);

        assertThrows(RegraDeNegocioException.class, () -> servico.excluir(CANDIDATO));
        verify(autenticacao, never()).removerConta(anyLong());
    }

    @Test
    void excluirConverteFalhaDoBancoEmConflito() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.of(candidato()));
        doThrow(new JpaSystemException(new RuntimeException("FOREIGN KEY constraint failed")))
                .when(candidatos).flush();

        assertThrows(ConflitoException.class, () -> servico.excluir(CANDIDATO));
    }

    @Test
    void excluirCandidatoInexistenteLancaNaoEncontrado() {
        when(candidatos.findById(CANDIDATO)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.excluir(CANDIDATO));
        verify(autenticacao, never()).removerConta(anyLong());
    }

    @Test
    void cadastrarPartidoGuardaSiglaENome() {
        when(partidos.save(any(Partido.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PartidoDto dto = servico.cadastrarPartido(new NovoPartido("PXT", "Partido X"));

        assertEquals("PXT", dto.sigla());
        assertEquals("Partido X", dto.nome());
    }

    @Test
    void cadastrarPartidoRecusaSiglaRepetida() {
        when(partidos.contarPorSigla("PXT")).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.cadastrarPartido(new NovoPartido("PXT", "Partido X")));
        verify(partidos, never()).save(any());
    }

    private NovoCandidato novoCandidato() {
        return new NovoCandidato("João", 15, PARTIDO, Cargo.GOVERNADOR, ELEICAO, "joao", "senha");
    }

    private DadosCandidato dadosCandidato(String nome) {
        return new DadosCandidato(nome, 15, PARTIDO, Cargo.GOVERNADOR);
    }

    private Candidato candidato() {
        return new Candidato(USUARIO, "João", 15, PARTIDO, Cargo.GOVERNADOR, ELEICAO);
    }
}
