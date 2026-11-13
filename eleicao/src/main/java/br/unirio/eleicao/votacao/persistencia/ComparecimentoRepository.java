package br.unirio.eleicao.votacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComparecimentoRepository extends JpaRepository<Comparecimento, ComparecimentoId> {
    // Somente a transacao que inserir o comparecimento pode contabilizar o voto.
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into comparecimento (eleitor_id, eleicao_id, cargo_id) "
            + "values (:eleitorId, :eleicaoId, :cargoId) "
            + "on conflict (eleitor_id, eleicao_id, cargo_id) do nothing", nativeQuery = true)
    int inserirSeAusente(@Param("eleitorId") Long eleitorId, @Param("eleicaoId") Long eleicaoId,
                         @Param("cargoId") Long cargoId);
}
