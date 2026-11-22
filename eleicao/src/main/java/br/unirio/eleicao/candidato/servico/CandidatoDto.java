package br.unirio.eleicao.candidato.servico;

import br.unirio.eleicao.shared.Cargo;

import java.time.LocalDate;

public record CandidatoDto(Long id, String nome, int numero, String partido, Cargo cargo, Long eleicaoId,
                           LocalDate dataNascimento) {
}
