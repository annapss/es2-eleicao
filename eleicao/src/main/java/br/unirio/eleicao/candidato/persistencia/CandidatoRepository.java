package br.unirio.eleicao.candidato.persistencia;

import br.unirio.eleicao.shared.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidatoRepository extends JpaRepository<Candidato, Long> {
    @Query("select count(c) from Candidato c where c.eleicaoId = :eleicaoId and c.cargo = :cargo and c.numero = :numero")
    long contarPorNumero(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo,
                         @Param("numero") int numero);

    @Query("select count(c) from Candidato c where c.eleicaoId = :eleicaoId and c.cargo = :cargo"
            + " and c.numero = :numero and c.id <> :id")
    long contarPorNumeroDeOutroCandidato(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo,
                                         @Param("numero") int numero, @Param("id") Long id);

    @Query("select c from Candidato c where (:eleicaoId is null or c.eleicaoId = :eleicaoId)"
            + " and (:cargo is null or c.cargo = :cargo) order by c.eleicaoId, c.cargo, c.numero")
    List<Candidato> listarPorFiltro(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo);

    @Query("select c from Candidato c where c.usuarioId = :usuarioId and c.eleicaoId = :eleicaoId")
    Optional<Candidato> buscarPorUsuarioEEleicao(@Param("usuarioId") Long usuarioId,
                                                 @Param("eleicaoId") Long eleicaoId);
}
