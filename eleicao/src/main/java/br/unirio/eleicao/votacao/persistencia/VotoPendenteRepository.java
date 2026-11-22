package br.unirio.eleicao.votacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoPendenteRepository extends JpaRepository<VotoPendente, VotoPendenteId> {
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into voto_pendente (eleitor_id, eleicao_id, cargo_id, candidato_id) "
            + "values (:eleitorId, :eleicaoId, :cargoId, :candidatoId) "
            + "on conflict (eleitor_id, eleicao_id, cargo_id) do nothing", nativeQuery = true)
    int inserirSeAusente(@Param("eleitorId") Long eleitorId, @Param("eleicaoId") Long eleicaoId,
                         @Param("cargoId") Long cargoId, @Param("candidatoId") Long candidatoId);
}
