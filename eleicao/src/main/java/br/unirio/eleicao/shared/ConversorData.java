package br.unirio.eleicao.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class ConversorData implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate data) {
        return data == null ? null : data.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String texto) {
        return texto == null ? null : LocalDate.parse(texto);
    }
}
