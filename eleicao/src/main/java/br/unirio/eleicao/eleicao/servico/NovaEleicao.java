package br.unirio.eleicao.eleicao.servico;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record NovaEleicao(
        @NotNull LocalDate data,
        @NotNull @Min(1) @Max(2) Integer turno,
        @NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "08:00") LocalTime horaAbertura,
        @NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "17:00") LocalTime horaEncerramento) {
}
