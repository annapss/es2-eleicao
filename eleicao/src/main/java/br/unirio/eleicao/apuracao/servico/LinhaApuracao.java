package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.shared.Cargo;

public record LinhaApuracao(Cargo cargo, Long candidatoId, int numero, String candidato, String partido,
                            long totalVotos) {
}
