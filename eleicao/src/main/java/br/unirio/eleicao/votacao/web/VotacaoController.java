package br.unirio.eleicao.votacao.web;

import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.votacao.servico.VotacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votacao")
@Tag(name = "Votação")
public class VotacaoController {
    private final VotacaoServico servico;

    public VotacaoController(VotacaoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Consultar candidatos disponíveis para votação",
            description = "Acesso exclusivo de eleitores. Retorna candidatos da eleição ABERTA, agrupados "
                    + "por cargo e ordenados por número. Cargos sem candidatos retornam listas vazias. "
                    + "Eleição inexistente retorna 404; AGENDADA ou ENCERRADA retorna 422.")
    @GetMapping("/{eleicaoId}/candidatos")
    public Map<Cargo, List<CandidatoDto>> listarCandidatos(@PathVariable Long eleicaoId) {
        return servico.listarCandidatos(eleicaoId);
    }
}
