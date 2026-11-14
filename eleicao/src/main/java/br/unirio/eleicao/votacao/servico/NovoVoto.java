package br.unirio.eleicao.votacao.servico;

import br.unirio.eleicao.shared.Cargo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NovoVoto(@NotNull Cargo cargo, @NotNull @Positive Long candidatoId) {
}
