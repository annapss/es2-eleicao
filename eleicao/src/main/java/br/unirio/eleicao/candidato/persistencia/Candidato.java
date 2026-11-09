package br.unirio.eleicao.candidato.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "candidato",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_candidato_numero", columnNames = {"eleicao_id", "cargo_id", "numero"}),
                @UniqueConstraint(name = "uq_candidato_usuario_eleicao", columnNames = {"usuario_id", "eleicao_id"}),
                @UniqueConstraint(name = "uq_candidato_id_eleicao_cargo",
                        columnNames = {"id", "eleicao_id", "cargo_id"})
        },
        indexes = @Index(name = "ix_candidato_eleicao_cargo", columnList = "eleicao_id, cargo_id"))
public class Candidato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String perfil = "CANDIDATO";

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int numero;

    @Column(name = "partido_id", nullable = false)
    private Long partidoId;

    @Column(name = "cargo_id", nullable = false)
    private Long cargoId;

    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    protected Candidato() {
    }

    public Candidato(Long usuarioId, String nome, int numero, Long partidoId, Long cargoId, Long eleicaoId) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.numero = numero;
        this.partidoId = partidoId;
        this.cargoId = cargoId;
        this.eleicaoId = eleicaoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public Long getPartidoId() {
        return partidoId;
    }

    public void setPartidoId(Long partidoId) {
        this.partidoId = partidoId;
    }

    public Long getCargoId() {
        return cargoId;
    }

    public void setCargoId(Long cargoId) {
        this.cargoId = cargoId;
    }

    public Long getEleicaoId() {
        return eleicaoId;
    }

    public void setEleicaoId(Long eleicaoId) {
        this.eleicaoId = eleicaoId;
    }
}
