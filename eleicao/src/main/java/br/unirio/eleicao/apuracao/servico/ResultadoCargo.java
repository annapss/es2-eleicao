package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.shared.Cargo;

import java.util.List;

// Resultado de um cargo: candidatos ordenados do mais votado para o menos votado.
// maisVotado é nulo quando o cargo não recebeu nenhum voto.
// desempatePorIdade indica que houve empate em votos e o vencedor foi o candidato mais velho.
public record ResultadoCargo(Cargo cargo, LinhaApuracao maisVotado, boolean desempatePorIdade,
                             List<LinhaApuracao> candidatos) {
}
