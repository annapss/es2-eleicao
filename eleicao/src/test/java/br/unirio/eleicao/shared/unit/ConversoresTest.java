package br.unirio.eleicao.shared.unit;

import br.unirio.eleicao.shared.ConversorData;
import br.unirio.eleicao.shared.ConversorHora;
import br.unirio.eleicao.shared.ConversorMomento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversoresTest {

    @Test
    void dataUsaOFormatoDoSchema() {
        ConversorData conversor = new ConversorData();
        LocalDate data = LocalDate.of(2099, 10, 4);

        assertEquals("2099-10-04", conversor.convertToDatabaseColumn(data));
        assertEquals(data, conversor.convertToEntityAttribute("2099-10-04"));
    }

    @Test
    void horaUsaOFormatoDoSchema() {
        ConversorHora conversor = new ConversorHora();
        LocalTime hora = LocalTime.of(8, 0);

        assertEquals("08:00", conversor.convertToDatabaseColumn(hora));
        assertEquals(hora, conversor.convertToEntityAttribute("08:00"));
    }

    @Test
    void momentoUsaOFormatoDoSchema() {
        ConversorMomento conversor = new ConversorMomento();
        LocalDateTime momento = LocalDateTime.of(2099, 10, 4, 8, 30, 15);

        assertEquals("2099-10-04 08:30:15", conversor.convertToDatabaseColumn(momento));
        assertEquals(momento, conversor.convertToEntityAttribute("2099-10-04 08:30:15"));
    }

    @Test
    void valorNuloContinuaNulo() {
        assertNull(new ConversorMomento().convertToDatabaseColumn(null));
        assertNull(new ConversorMomento().convertToEntityAttribute(null));
    }
}
