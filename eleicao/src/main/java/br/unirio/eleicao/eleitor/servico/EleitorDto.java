package br.unirio.eleicao.eleitor.servico;

import java.time.LocalDate;

public record EleitorDto(Long id, String nome, String cpf, String tituloEleitor, LocalDate dataNascimento) {
}
