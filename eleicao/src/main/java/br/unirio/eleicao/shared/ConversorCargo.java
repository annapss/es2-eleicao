package br.unirio.eleicao.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConversorCargo implements AttributeConverter<Cargo, Long> {

    @Override
    public Long convertToDatabaseColumn(Cargo cargo) {
        return cargo == null ? null : cargo.getId();
    }

    @Override
    public Cargo convertToEntityAttribute(Long id) {
        return id == null ? null : Cargo.deId(id);
    }
}
