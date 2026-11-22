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
@Table(name = "voto_pendente")
@IdClass(VotoPendenteId.class)
public class VotoPendente {
    @Id
    @Column(name = "eleitor_id", nullable = false)
    private Long eleitorId;

    @Id
    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    @Id
    @Column(name = "cargo_id", nullable = false)
    private Long cargoId;

    @Column(name = "candidato_id", nullable = false)
    private Long candidatoId;

    @Convert(converter = ConversorMomento.class)
    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    protected VotoPendente() {
    }

    public VotoPendente(Long eleitorId, Long eleicaoId, Long cargoId, Long candidatoId) {
        this.eleitorId = eleitorId;
        this.eleicaoId = eleicaoId;
        this.cargoId = cargoId;
        this.candidatoId = candidatoId;
    }

    @PrePersist
    void definirMomentoRegistro() {
        if (registradoEm == null) {
            registradoEm = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
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

    public Long candidatoId() {
        return candidatoId;
    }

    public LocalDateTime registradoEm() {
        return registradoEm;
    }
}
