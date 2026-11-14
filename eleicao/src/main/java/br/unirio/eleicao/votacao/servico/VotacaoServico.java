package br.unirio.eleicao.votacao.servico;

import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.StatusEleicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class VotacaoServico {
    private final EleicaoServico eleicao;
    private final CandidatoServico candidatos;

    public VotacaoServico(EleicaoServico eleicao, CandidatoServico candidatos) {
        this.eleicao = eleicao;
        this.candidatos = candidatos;
    }

    @Transactional(readOnly = true)
    public Map<Cargo, List<CandidatoDto>> listarCandidatos(Long eleicaoId) {
        eleicao.exigirStatus(eleicaoId, StatusEleicao.ABERTA);
        Map<Cargo, List<CandidatoDto>> porCargo = new EnumMap<>(Cargo.class);
        for (Cargo cargo : Cargo.values()) {
            porCargo.put(cargo, new ArrayList<>());
        }
        for (CandidatoDto candidato : candidatos.listar(eleicaoId, null)) {
            porCargo.get(candidato.cargo()).add(candidato);
        }
        return porCargo;
    }
}
