package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.shared.Cargo;

import java.util.List;

public record ResultadoCargo(Cargo cargo, LinhaApuracao maisVotado, boolean desempatePorIdade,
                             List<LinhaApuracao> candidatos) {
}
