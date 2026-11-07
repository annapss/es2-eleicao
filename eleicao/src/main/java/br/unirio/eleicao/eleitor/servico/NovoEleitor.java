package br.unirio.eleicao.eleitor.servico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record NovoEleitor(
        @NotBlank String nome,
        @NotNull @Pattern(regexp = "\\d{11}") String cpf,
        @NotBlank String tituloEleitor,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank String login,
        @NotBlank String senha) {
}
