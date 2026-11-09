package br.unirio.eleicao.candidato.persistencia;

import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.ConversorCargo;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidato")
public class Candidato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int numero;

    @Column(name = "partido_id", nullable = false)
    private Long partidoId;

    @Convert(converter = ConversorCargo.class)
    @Column(name = "cargo_id", nullable = false)
    private Cargo cargo;

    @Column(name = "eleicao_id", nullable = false)
    private Long eleicaoId;

    protected Candidato() {
    }

    public Candidato(Long usuarioId, String nome, int numero, Long partidoId, Cargo cargo, Long eleicaoId) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.numero = numero;
        this.partidoId = partidoId;
        this.cargo = cargo;
        this.eleicaoId = eleicaoId;
    }

    public Long id() {
        return id;
    }

    public Long usuarioId() {
        return usuarioId;
    }

    public String nome() {
        return nome;
    }

    public int numero() {
        return numero;
    }

    public Long partidoId() {
        return partidoId;
    }

    public Cargo cargo() {
        return cargo;
    }

    public Long eleicaoId() {
        return eleicaoId;
    }

    public void alterar(String nome, int numero, Long partidoId, Cargo cargo) {
        this.nome = nome;
        this.numero = numero;
        this.partidoId = partidoId;
        this.cargo = cargo;
    }
}
