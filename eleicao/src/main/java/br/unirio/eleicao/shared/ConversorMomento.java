package br.unirio.eleicao.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter
public class ConversorMomento implements AttributeConverter<LocalDateTime, String> {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime momento) {
        return momento == null ? null : momento.format(FORMATO);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String texto) {
        return texto == null ? null : LocalDateTime.parse(texto, FORMATO);
    }
}
