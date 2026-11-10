package br.unirio.eleicao.candidato.web;

import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.candidato.servico.DadosCandidato;
import br.unirio.eleicao.candidato.servico.NovoCandidato;
import br.unirio.eleicao.shared.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/candidatos")
@Tag(name = "Candidatos")
public class CandidatoController {

    private final CandidatoServico servico;

    public CandidatoController(CandidatoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Cadastrar candidato",
            description = "Associa o candidato a um cargo e a uma eleição AGENDADA e cria a conta de login. "
                    + "Eleição fora de AGENDADA retorna 422; eleição ou partido inexistente retornam 404; "
                    + "número repetido no mesmo cargo e eleição ou login repetido retornam 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CandidatoDto cadastrar(@Valid @RequestBody NovoCandidato dados) {
        return servico.cadastrar(dados);
    }

    @Operation(summary = "Listar candidatos", description = "Filtros opcionais por eleição e por cargo.")
    @GetMapping
    public List<CandidatoDto> listar(@RequestParam(required = false) Long eleicaoId,
                                     @RequestParam(required = false) Cargo cargo) {
        return servico.listar(eleicaoId, cargo);
    }

    @Operation(summary = "Buscar candidato por id", description = "Candidato inexistente retorna 404.")
    @GetMapping("/{id}")
    public CandidatoDto buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Alterar candidato",
            description = "Altera nome, número, partido e cargo. A eleição não muda. Só com a eleição AGENDADA; "
                    + "caso contrário retorna 422.")
    @PutMapping("/{id}")
    public CandidatoDto atualizar(@PathVariable Long id, @Valid @RequestBody DadosCandidato dados) {
        return servico.atualizar(id, dados);
    }

    @Operation(summary = "Excluir candidato",
            description = "Remove o candidato e a conta de login. Só com a eleição AGENDADA; caso contrário "
                    + "retorna 422. Candidato com votos retorna 409.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        servico.excluir(id);
    }
}
