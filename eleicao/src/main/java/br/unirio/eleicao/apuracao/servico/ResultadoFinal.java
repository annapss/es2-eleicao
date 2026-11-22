package br.unirio.eleicao.apuracao.servico;

import java.util.List;

public record ResultadoFinal(Long eleicaoId, List<ResultadoCargo> cargos) {
}
