package br.unirio.eleicao.votacao.unit;

import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import br.unirio.eleicao.votacao.servico.VotacaoServico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotacaoServicoTest {
    @Mock private EleicaoServico eleicao;
    @Mock private CandidatoServico candidatos;
    @InjectMocks private VotacaoServico servico;

    @Test
    void agrupaCandidatosPorCargoIncluindoCargosVazios() {
        CandidatoDto governador = new CandidatoDto(1L, "Ana", 15, "PTST", Cargo.GOVERNADOR, 7L);
        CandidatoDto senador = new CandidatoDto(2L, "Bia", 151, "PTST", Cargo.SENADOR, 7L);
        when(candidatos.listar(7L, null)).thenReturn(List.of(governador, senador));

        var resultado = servico.listarCandidatos(7L);

        assertEquals(Cargo.values().length, resultado.size());
        assertEquals(List.of(governador), resultado.get(Cargo.GOVERNADOR));
        assertEquals(List.of(senador), resultado.get(Cargo.SENADOR));
        assertTrue(resultado.get(Cargo.PRESIDENTE).isEmpty());
        verify(eleicao).exigirStatus(7L, StatusEleicao.ABERTA);
    }

    @Test
    void naoConsultaCandidatosQuandoEleicaoNaoEstaAberta() {
        doThrow(new RegraDeNegocioException("Eleicao nao esta aberta"))
                .when(eleicao).exigirStatus(7L, StatusEleicao.ABERTA);

        assertThrows(RegraDeNegocioException.class, () -> servico.listarCandidatos(7L));

        verifyNoInteractions(candidatos);
    }
}
