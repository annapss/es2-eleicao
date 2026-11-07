package br.unirio.eleicao.eleicao.servico;

import br.unirio.eleicao.shared.StatusEleicao;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

public record EleicaoDto(
        Long id,
        LocalDate data,
        int turno,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "08:00") LocalTime horaAbertura,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "17:00") LocalTime horaEncerramento,
        StatusEleicao status) {
}
