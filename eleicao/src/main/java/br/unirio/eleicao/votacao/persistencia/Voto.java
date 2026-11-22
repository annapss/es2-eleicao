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

    public String id() {
        return id;
    }

    public Long eleicaoId() {
        return eleicaoId;
    }

    public Long cargoId() {
        return cargoId;
    }

    public Long candidatoId() {
        return candidatoId;
    }
}
