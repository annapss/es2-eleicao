package br.unirio.eleicao.apuracao.web;

import br.unirio.eleicao.apuracao.servico.ApuracaoServico;
import br.unirio.eleicao.apuracao.servico.LinhaApuracao;
import br.unirio.eleicao.apuracao.servico.ResultadoFinal;
import br.unirio.eleicao.shared.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/apuracao")
@Tag(name = "Apuração")
public class ApuracaoController {
    private final ApuracaoServico servico;

    public ApuracaoController(ApuracaoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Acompanhar a votação",
            description = "Votos confirmados por candidato na eleição ABERTA, atualizados a cada consulta. "
                    + "Sem o filtro cargo, retorna todos os cargos, ordenados por cargo e por votos. "
                    + "Candidatos sem voto aparecem com zero. Votos pendentes não contam. "
                    + "Eleitor e Chefe da Seção veem todos os cargos. O candidato vê apenas o cargo que disputa: "
                    + "sem o filtro, recebe o próprio cargo; outro cargo, ou eleição em que não concorre, retorna 403. "
                    + "Eleição inexistente retorna 404; AGENDADA ou ENCERRADA retorna 422; cargo inválido 400.")
    @GetMapping("/eleicoes/{eleicaoId}/parcial")
    public List<LinhaApuracao> parcial(@PathVariable Long eleicaoId,
                                       @RequestParam(required = false) Cargo cargo) {
        return servico.parcial(eleicaoId, cargo);
    }

    @Operation(summary = "Consultar o resultado final",
            description = "Disponível somente com a eleição ENCERRADA. Retorna todos os cargos, na ordem oficial, "
                    + "cada um com seus candidatos (cargo, número, partido e votos), do mais votado para o menos "
                    + "votado. Cargos sem candidatos vêm com lista vazia. Eleitor, Candidato e Chefe da Seção veem "
                    + "todos os cargos. Eleição inexistente retorna 404; AGENDADA ou ABERTA retorna 422.")
    @GetMapping("/eleicoes/{eleicaoId}/resultado")
    public ResultadoFinal resultado(@PathVariable Long eleicaoId) {
        return servico.resultado(eleicaoId);
    }
}
