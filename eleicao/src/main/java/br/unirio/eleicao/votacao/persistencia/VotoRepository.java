package br.unirio.eleicao.votacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, String> {
}
