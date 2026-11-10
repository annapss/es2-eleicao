package br.unirio.eleicao.candidato.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartidoRepository extends JpaRepository<Partido, Long> {
    @Query("select count(p) from Partido p where p.sigla = :sigla")
    long contarPorSigla(@Param("sigla") String sigla);
}
