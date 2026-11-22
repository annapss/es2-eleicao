package br.unirio.eleicao.votacao.persistencia;

import java.io.Serializable;
import java.util.Objects;

public class ComparecimentoId implements Serializable {
    private Long eleitorId;
    private Long eleicaoId;
    private Long cargoId;

    public ComparecimentoId() {
    }

    public ComparecimentoId(Long eleitorId, Long eleicaoId, Long cargoId) {
        this.eleitorId = eleitorId;
        this.eleicaoId = eleicaoId;
        this.cargoId = cargoId;
    }

    public Long eleitorId() {
        return eleitorId;
    }

    public Long eleicaoId() {
        return eleicaoId;
    }

    public Long cargoId() {
        return cargoId;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof ComparecimentoId that)) {
            return false;
        }
        return Objects.equals(eleitorId, that.eleitorId)
                && Objects.equals(eleicaoId, that.eleicaoId)
                && Objects.equals(cargoId, that.cargoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eleitorId, eleicaoId, cargoId);
    }
}
