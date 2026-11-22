package br.unirio.eleicao.votacao.persistencia;

import br.unirio.eleicao.shared.ConversorMomento;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "comparecimento")
@IdClass(ComparecimentoId.class)
public class Comparecimento {
    @Id
    @Column(name = "eleitor_id", nullable = false)
    private Long eleitorId;

    @Id
    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    @Id
    @Column(name = "cargo_id", nullable = false)
    private Long cargoId;

    @Convert(converter = ConversorMomento.class)
    @Column(name = "votou_em", nullable = false)
    private LocalDateTime votouEm;

    protected Comparecimento() {
    }

    public Comparecimento(Long eleitorId, Long eleicaoId, Long cargoId) {
        this.eleitorId = eleitorId;
        this.eleicaoId = eleicaoId;
        this.cargoId = cargoId;
    }

    @PrePersist
    void definirMomentoComparecimento() {
        if (votouEm == null) {
            votouEm = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
        }
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

    public LocalDateTime votouEm() {
        return votouEm;
    }
}
