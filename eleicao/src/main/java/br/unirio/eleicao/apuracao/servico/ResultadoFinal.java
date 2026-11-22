package br.unirio.eleicao.apuracao.servico;

import java.util.List;

// Resultado final de uma eleição ENCERRADA, com um item por cargo na ordem oficial dos cargos.
public record ResultadoFinal(Long eleicaoId, List<ResultadoCargo> cargos) {
}
