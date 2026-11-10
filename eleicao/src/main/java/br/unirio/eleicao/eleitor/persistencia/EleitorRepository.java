package br.unirio.eleicao.eleitor.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EleitorRepository extends JpaRepository<Eleitor, Long> {
    @Query("select count(e) from Eleitor e where e.cpf = :cpf")
    long contarPorCpf(@Param("cpf") String cpf);

    @Query("select count(e) from Eleitor e where e.tituloEleitor = :titulo")
    long contarPorTitulo(@Param("titulo") String titulo);

    @Query("select count(e) from Eleitor e where e.cpf = :cpf and e.usuarioId <> :id")
    long contarPorCpfDeOutroEleitor(@Param("cpf") String cpf, @Param("id") Long id);

    @Query("select count(e) from Eleitor e where e.tituloEleitor = :titulo and e.usuarioId <> :id")
    long contarPorTituloDeOutroEleitor(@Param("titulo") String titulo, @Param("id") Long id);
}
