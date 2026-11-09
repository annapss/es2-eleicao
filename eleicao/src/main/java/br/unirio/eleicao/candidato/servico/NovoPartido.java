package br.unirio.eleicao.candidato.servico;

import jakarta.validation.constraints.NotBlank;

public record NovoPartido(@NotBlank String sigla, @NotBlank String nome) {
}
