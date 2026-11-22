package br.unirio.eleicao.apuracao.persistencia;

import br.unirio.eleicao.shared.Cargo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApuracaoRepository extends Repository<ApuracaoCandidato, Long> {
    @Query("select a from ApuracaoCandidato a where a.eleicaoId = :eleicaoId"
            + " and (:cargo is null or a.cargo = :cargo)"
            + " order by a.cargoOrdem, a.totalVotos desc, a.numero")
    List<ApuracaoCandidato> listar(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo);
}
