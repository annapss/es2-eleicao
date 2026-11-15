package br.unirio.eleicao.apuracao.unit;

import br.unirio.eleicao.apuracao.persistencia.ApuracaoCandidato;
import br.unirio.eleicao.apuracao.persistencia.ApuracaoRepository;
import br.unirio.eleicao.apuracao.servico.ApuracaoServico;
import br.unirio.eleicao.apuracao.servico.LinhaApuracao;
import br.unirio.eleicao.apuracao.servico.ResultadoCargo;
import br.unirio.eleicao.apuracao.servico.ResultadoFinal;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApuracaoServicoTest {
    @Mock private ApuracaoRepository apuracao;
    @Mock private EleicaoServico eleicao;
    @Mock private CandidatoServico candidatos;
    @Mock private AutenticacaoServico autenticacao;
    @InjectMocks private ApuracaoServico servico;

    @Test
    void eleitorAcompanhaTodosOsCargos() {
        logado(Perfil.ELEITOR);
        ApuracaoCandidato governador = linha(1L, Cargo.GOVERNADOR, 15, "Ana", "PTST", 3);
        ApuracaoCandidato senador = linha(2L, Cargo.SENADOR, 151, "Bia", "PXYZ", 0);
        when(apuracao.listar(7L, null)).thenReturn(List.of(senador, governador));

        List<LinhaApuracao> resultado = servico.parcial(7L, null);

        assertEquals(List.of(
                new LinhaApuracao(Cargo.SENADOR, 2L, 151, "Bia", "PXYZ", 0),
                new LinhaApuracao(Cargo.GOVERNADOR, 1L, 15, "Ana", "PTST", 3)), resultado);
        verify(eleicao).exigirStatus(7L, StatusEleicao.ABERTA);
    }

    @Test
    void repassaFiltroDeCargo() {
        logado(Perfil.ELEITOR);
        when(apuracao.listar(7L, Cargo.PRESIDENTE)).thenReturn(List.of());

        assertTrue(servico.parcial(7L, Cargo.PRESIDENTE).isEmpty());

        verify(apuracao).listar(7L, Cargo.PRESIDENTE);
    }

    @Test
    void chefeDaSecaoTambemAcompanha() {
        logado(Perfil.CHEFE_SECAO);
        when(apuracao.listar(7L, null)).thenReturn(List.of());

        assertTrue(servico.parcial(7L, null).isEmpty());
    }

    @Test
    void naoConsultaQuandoEleicaoNaoEstaAberta() {
        logado(Perfil.ELEITOR);
        doThrow(new RegraDeNegocioException("Eleição não está aberta"))
                .when(eleicao).exigirStatus(7L, StatusEleicao.ABERTA);

        assertThrows(RegraDeNegocioException.class, () -> servico.parcial(7L, null));

        verifyNoInteractions(apuracao);
    }

    @Test
    void naoConsultaQuandoEleicaoNaoExiste() {
        logado(Perfil.ELEITOR);
        doThrow(new NaoEncontradoException("Eleição não encontrada"))
                .when(eleicao).exigirStatus(99L, StatusEleicao.ABERTA);

        assertThrows(NaoEncontradoException.class, () -> servico.parcial(99L, null));

        verifyNoInteractions(apuracao);
    }

    @Test
    void eleitorNaoPrecisaConsultarCandidatura() {
        logado(Perfil.ELEITOR);
        when(apuracao.listar(7L, Cargo.SENADOR)).thenReturn(List.of());

        servico.parcial(7L, Cargo.SENADOR);

        verifyNoInteractions(candidatos);
    }

    @Test
    void candidatoSemFiltroRecebeOProprioCargo() {
        logado(Perfil.CANDIDATO);
        concorre(Cargo.GOVERNADOR);
        ApuracaoCandidato governador = linha(1L, Cargo.GOVERNADOR, 15, "Ana", "PTST", 3);
        when(apuracao.listar(7L, Cargo.GOVERNADOR)).thenReturn(List.of(governador));

        List<LinhaApuracao> resultado = servico.parcial(7L, null);

        assertEquals(List.of(new LinhaApuracao(Cargo.GOVERNADOR, 1L, 15, "Ana", "PTST", 3)), resultado);
        verify(apuracao, never()).listar(7L, null);
    }

    @Test
    void candidatoPodePedirOProprioCargo() {
        logado(Perfil.CANDIDATO);
        concorre(Cargo.GOVERNADOR);
        when(apuracao.listar(7L, Cargo.GOVERNADOR)).thenReturn(List.of());

        assertTrue(servico.parcial(7L, Cargo.GOVERNADOR).isEmpty());
    }

    @Test
    void candidatoNaoVeOutroCargo() {
        logado(Perfil.CANDIDATO);
        concorre(Cargo.GOVERNADOR);

        assertThrows(AccessDeniedException.class, () -> servico.parcial(7L, Cargo.PRESIDENTE));

        verifyNoInteractions(apuracao);
    }

    @Test
    void candidatoNaoVeEleicaoEmQueNaoConcorre() {
        logado(Perfil.CANDIDATO);
        when(candidatos.buscarPorUsuario(1L, 7L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> servico.parcial(7L, null));

        verifyNoInteractions(apuracao);
    }

    @Test
    void candidatoTambemExigeEleicaoAberta() {
        logado(Perfil.CANDIDATO);
        doThrow(new RegraDeNegocioException("Eleição não está aberta"))
                .when(eleicao).exigirStatus(7L, StatusEleicao.ABERTA);

        assertThrows(RegraDeNegocioException.class, () -> servico.parcial(7L, null));

        verifyNoInteractions(candidatos, apuracao);
    }

    @Test
    void resultadoAgrupaPorCargoNaOrdemOficial() {
        ApuracaoCandidato senador = linha(2L, Cargo.SENADOR, 151, "Bia", "PXYZ", 4);
        ApuracaoCandidato governador1 = linha(1L, Cargo.GOVERNADOR, 15, "Ana", "PTST", 3);
        ApuracaoCandidato governador2 = linha(3L, Cargo.GOVERNADOR, 25, "Caio", "PXYZ", 1);
        when(apuracao.listar(7L, null)).thenReturn(List.of(senador, governador1, governador2));

        ResultadoFinal resultado = servico.resultado(7L);

        assertEquals(7L, resultado.eleicaoId());
        assertEquals(List.of(Cargo.values()), resultado.cargos().stream().map(ResultadoCargo::cargo).toList());
        assertEquals(List.of(new LinhaApuracao(Cargo.SENADOR, 2L, 151, "Bia", "PXYZ", 4)),
                resultado.cargos().get(2).candidatos());
        assertEquals(List.of(
                new LinhaApuracao(Cargo.GOVERNADOR, 1L, 15, "Ana", "PTST", 3),
                new LinhaApuracao(Cargo.GOVERNADOR, 3L, 25, "Caio", "PXYZ", 1)),
                resultado.cargos().get(3).candidatos());
        assertTrue(resultado.cargos().get(0).candidatos().isEmpty());
        verify(eleicao).exigirStatus(7L, StatusEleicao.ENCERRADA);
    }

    @Test
    void resultadoNaoDependeDoPerfil() {
        when(apuracao.listar(7L, null)).thenReturn(List.of());

        servico.resultado(7L);

        // Nenhuma restrição por perfil: o resultado final é igual para eleitor, candidato e chefe.
        verifyNoInteractions(autenticacao, candidatos);
    }

    @Test
    void resultadoExigeEleicaoEncerrada() {
        doThrow(new RegraDeNegocioException("Eleição não está encerrada"))
                .when(eleicao).exigirStatus(7L, StatusEleicao.ENCERRADA);

        assertThrows(RegraDeNegocioException.class, () -> servico.resultado(7L));

        verifyNoInteractions(apuracao);
    }

    private void concorre(Cargo cargo) {
        when(candidatos.buscarPorUsuario(1L, 7L))
                .thenReturn(Optional.of(new CandidatoDto(1L, "Ana", 15, "PTST", cargo, 7L)));
    }

    private void logado(Perfil perfil) {
        when(autenticacao.usuarioAtual()).thenReturn(new UsuarioAutenticado(1L, perfil));
    }

    private ApuracaoCandidato linha(Long id, Cargo cargo, int numero, String nome, String partido, long votos) {
        ApuracaoCandidato linha = mock(ApuracaoCandidato.class);
        when(linha.candidatoId()).thenReturn(id);
        when(linha.cargo()).thenReturn(cargo);
        when(linha.numero()).thenReturn(numero);
        when(linha.nome()).thenReturn(nome);
        when(linha.partido()).thenReturn(partido);
        when(linha.totalVotos()).thenReturn(votos);
        return linha;
    }
}
