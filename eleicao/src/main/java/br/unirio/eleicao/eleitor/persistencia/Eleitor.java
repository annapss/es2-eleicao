package br.unirio.eleicao.eleitor.persistencia;

import br.unirio.eleicao.shared.ConversorData;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "eleitor")
public class Eleitor {
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cpf;

    @Column(name = "titulo_eleitor", nullable = false)
    private String tituloEleitor;

    @Convert(converter = ConversorData.class)
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    protected Eleitor() {
    }

    public Eleitor(Long usuarioId, String nome, String cpf, String tituloEleitor, LocalDate dataNascimento) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.cpf = cpf;
        this.tituloEleitor = tituloEleitor;
        this.dataNascimento = dataNascimento;
    }

    public Long usuarioId() {
        return usuarioId;
    }

    public String nome() {
        return nome;
    }

    public String cpf() {
        return cpf;
    }

    public String tituloEleitor() {
        return tituloEleitor;
    }

    public LocalDate dataNascimento() {
        return dataNascimento;
    }

    public void alterar(String nome, String cpf, String tituloEleitor, LocalDate dataNascimento) {
        this.nome = nome;
        this.cpf = cpf;
        this.tituloEleitor = tituloEleitor;
        this.dataNascimento = dataNascimento;
    }
}
