package br.unirio.eleicao.candidato.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partido")
public class Partido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sigla;

    @Column(nullable = false)
    private String nome;

    protected Partido() {
    }

    public Partido(String sigla, String nome) {
        this.sigla = sigla;
        this.nome = nome;
    }

    public Long id() {
        return id;
    }

    public String sigla() {
        return sigla;
    }

    public String nome() {
        return nome;
    }
}
