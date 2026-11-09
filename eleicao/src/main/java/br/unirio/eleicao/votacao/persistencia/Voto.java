package br.unirio.eleicao.votacao.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "voto")
public class Voto {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    @Column(name = "cargo_id", nullable = false)
    private Long cargoId;

    @Column(name = "candidato_id", nullable = false)
    private Long candidatoId;

    protected Voto() {
    }

    public Voto(Long eleicaoId, Long cargoId, Long candidatoId) {
        this.eleicaoId = eleicaoId;
        this.cargoId = cargoId;
        this.candidatoId = candidatoId;
    }

    @PrePersist
    void gerarIdentificador() {
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Long getCandidatoId() {
        return candidatoId;
    }

    public void setCandidatoId(Long candidatoId) {
        this.candidatoId = candidatoId;
    }
}
