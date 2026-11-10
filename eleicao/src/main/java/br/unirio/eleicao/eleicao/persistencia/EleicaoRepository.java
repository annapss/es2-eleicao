package br.unirio.eleicao.eleicao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface EleicaoRepository extends JpaRepository<Eleicao, Long> {
    @Query("select count(e) from Eleicao e where e.dataEleicao = :data and e.turno = :turno")
    long contarPorDataETurno(@Param("data") LocalDate data, @Param("turno") int turno);
}
