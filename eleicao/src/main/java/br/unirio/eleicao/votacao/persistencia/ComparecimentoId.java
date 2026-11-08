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

    public Long getEleitorId() {
        return eleitorId;
    }

    public void setEleitorId(Long eleitorId) {
        this.eleitorId = eleitorId;
    }

    public Long getEleicaoId() {
        return eleicaoId;
    }

    public void setEleicaoId(Long eleicaoId) {
        this.eleicaoId = eleicaoId;
    }

    public Long getCargoId() {
        return cargoId;
    }

    public void setCargoId(Long cargoId) {
        this.cargoId = cargoId;
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
