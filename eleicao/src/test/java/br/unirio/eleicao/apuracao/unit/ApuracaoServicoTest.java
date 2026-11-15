package br.unirio.eleicao.apuracao.unit;

import br.unirio.eleicao.apuracao.persistencia.ApuracaoCandidato;
import br.unirio.eleicao.apuracao.persistencia.ApuracaoRepository;
import br.unirio.eleicao.apuracao.servico.ApuracaoServico;
import br.unirio.eleicao.apuracao.servico.LinhaApuracao;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApuracaoServicoTest {
    @Mock private ApuracaoRepository apuracao;
    @Mock private EleicaoServico eleicao;
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
    void candidatoAindaNaoAcompanha() {
        logado(Perfil.CANDIDATO);

        assertThrows(AccessDeniedException.class, () -> servico.parcial(7L, null));

        verifyNoInteractions(eleicao, apuracao);
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
