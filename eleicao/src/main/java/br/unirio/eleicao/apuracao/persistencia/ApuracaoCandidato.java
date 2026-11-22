package br.unirio.eleicao.apuracao.persistencia;

import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.ConversorCargo;
import br.unirio.eleicao.shared.ConversorData;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDate;

@Entity
@Immutable
@Synchronize({"candidato", "cargo", "partido", "voto"})
@Table(name = "vw_apuracao")
public class ApuracaoCandidato {
    @Id
    @Column(name = "candidato_id")
    private Long candidatoId;

    @Column(name = "eleicao_id")
    private Long eleicaoId;

    @Convert(converter = ConversorCargo.class)
    @Column(name = "cargo_id")
    private Cargo cargo;

    @Column(name = "cargo_ordem")
    private int cargoOrdem;

    @Column(name = "numero")
    private int numero;

    @Column(name = "candidato")
    private String nome;

    @Column(name = "partido")
    private String partido;

    @Column(name = "total_votos")
    private long totalVotos;

    @Convert(converter = ConversorData.class)
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    protected ApuracaoCandidato() {
    }

    public Long candidatoId() {
        return candidatoId;
    }

    public Long eleicaoId() {
        return eleicaoId;
    }

    public Cargo cargo() {
        return cargo;
    }

    public int numero() {
        return numero;
    }

    public String nome() {
        return nome;
    }

    public String partido() {
        return partido;
    }

    public long totalVotos() {
        return totalVotos;
    }

    public LocalDate dataNascimento() {
        return dataNascimento;
    }
}
