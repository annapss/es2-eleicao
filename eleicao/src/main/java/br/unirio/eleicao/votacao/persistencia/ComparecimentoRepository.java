package br.unirio.eleicao.votacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparecimentoRepository extends JpaRepository<Comparecimento, ComparecimentoId> {
}
