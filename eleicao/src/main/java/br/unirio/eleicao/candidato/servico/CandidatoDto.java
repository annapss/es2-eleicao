package br.unirio.eleicao.candidato.servico;

import br.unirio.eleicao.shared.Cargo;

public record CandidatoDto(Long id, String nome, int numero, String partido, Cargo cargo, Long eleicaoId) {
}
