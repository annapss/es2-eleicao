package br.unirio.eleicao.cargo.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cargo", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cargo_nome", columnNames = "nome"),
        @UniqueConstraint(name = "uq_cargo_ordem", columnNames = "ordem")
})
public class Cargo {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false, unique = true)
    private int ordem;

    protected Cargo() {
    }

    public Cargo(Long id, String nome, int ordem) {
        this.id = id;
        this.nome = nome;
        this.ordem = ordem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }
}
