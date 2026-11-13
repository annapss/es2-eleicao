package br.unirio.eleicao.votacao.web;

import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.votacao.servico.NovoVoto;
import br.unirio.eleicao.votacao.servico.VotacaoServico;
import br.unirio.eleicao.votacao.servico.VotoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votacao")
@Tag(name = "Votação")
public class VotacaoController {
    private final VotacaoServico servico;
    private final VotoServico votos;

    public VotacaoController(VotacaoServico servico, VotoServico votos) {
        this.servico = servico;
        this.votos = votos;
    }

    @Operation(summary = "Consultar candidatos disponíveis para votação",
            description = "Acesso exclusivo de eleitores. Retorna candidatos da eleição ABERTA, agrupados "
                    + "por cargo e ordenados por número. Cargos sem candidatos retornam listas vazias. "
                    + "Eleição inexistente retorna 404; AGENDADA ou ENCERRADA retorna 422.")
    @GetMapping("/{eleicaoId}/candidatos")
    public Map<Cargo, List<CandidatoDto>> listarCandidatos(@PathVariable Long eleicaoId) {
        return servico.listarCandidatos(eleicaoId);
    }

    @Operation(summary = "Registrar voto pendente",
            description = "Exclusivo de eleitores cadastrados. Registra uma escolha por cargo na eleição ABERTA. "
                    + "O candidato deve pertencer ao cargo e à eleição. A escolha fica pendente de confirmação "
                    + "e ainda não conta na apuração. Retorna 201 sem corpo; dados inválidos retornam 400, "
                    + "eleição ou candidato inexistente 404, voto repetido ou já confirmado 409, "
                    + "eleição não aberta ou candidato de outro cargo/eleição 422.")
    @PostMapping("/{eleicaoId}/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@PathVariable Long eleicaoId, @Valid @RequestBody NovoVoto dados) {
        votos.registrar(eleicaoId, dados);
    }
}
