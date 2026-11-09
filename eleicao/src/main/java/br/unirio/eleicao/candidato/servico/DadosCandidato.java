package br.unirio.eleicao.candidato.servico;

import br.unirio.eleicao.shared.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DadosCandidato(
        @NotBlank String nome,
        @NotNull @Positive Integer numero,
        @NotNull Long partidoId,
        @NotNull Cargo cargo) {
}
