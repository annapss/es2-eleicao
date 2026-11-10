package br.unirio.eleicao.eleicao.persistencia;

import br.unirio.eleicao.shared.ConversorData;
import br.unirio.eleicao.shared.ConversorHora;
import br.unirio.eleicao.shared.ConversorMomento;
import br.unirio.eleicao.shared.StatusEleicao;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "eleicao")
public class Eleicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = ConversorData.class)
    @Column(name = "data_eleicao", nullable = false)
    private LocalDate dataEleicao;

    @Column(nullable = false)
    private int turno;

    @Convert(converter = ConversorHora.class)
    @Column(name = "hora_abertura", nullable = false)
    private LocalTime horaAbertura;

    @Convert(converter = ConversorHora.class)
    @Column(name = "hora_encerramento", nullable = false)
    private LocalTime horaEncerramento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEleicao status = StatusEleicao.AGENDADA;

    @Convert(converter = ConversorMomento.class)
    @Column(name = "aberta_em")
    private LocalDateTime abertaEm;

    @Convert(converter = ConversorMomento.class)
    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    @Column(name = "criada_por", nullable = false)
    private Long criadaPor;

    protected Eleicao() {
    }

    public Eleicao(LocalDate dataEleicao, int turno, LocalTime horaAbertura, LocalTime horaEncerramento,
                   Long criadaPor) {
        this.dataEleicao = dataEleicao;
        this.turno = turno;
        this.horaAbertura = horaAbertura;
        this.horaEncerramento = horaEncerramento;
        this.criadaPor = criadaPor;
    }

    public Long id() {
        return id;
    }

    public LocalDate dataEleicao() {
        return dataEleicao;
    }

    public int turno() {
        return turno;
    }

    public LocalTime horaAbertura() {
        return horaAbertura;
    }

    public LocalTime horaEncerramento() {
        return horaEncerramento;
    }

    public StatusEleicao status() {
        return status;
    }

    public Long criadaPor() {
        return criadaPor;
    }

    public void registrarAbertura(LocalDateTime momento) {
        this.status = StatusEleicao.ABERTA;
        this.abertaEm = momento;
    }

    public void registrarEncerramento(LocalDateTime momento) {
        this.status = StatusEleicao.ENCERRADA;
        this.encerradaEm = momento;
    }
}
