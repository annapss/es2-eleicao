package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.shared.Cargo;

import java.util.List;

// Resultado de um cargo: candidatos ordenados do mais votado para o menos votado.
public record ResultadoCargo(Cargo cargo, List<LinhaApuracao> candidatos) {
}
