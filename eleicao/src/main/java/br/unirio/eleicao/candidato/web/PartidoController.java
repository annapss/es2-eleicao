package br.unirio.eleicao.candidato.web;

import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.candidato.servico.NovoPartido;
import br.unirio.eleicao.candidato.servico.PartidoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/partidos")
@Tag(name = "Partidos")
public class PartidoController {

    private final CandidatoServico servico;

    public PartidoController(CandidatoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Cadastrar partido", description = "Sigla repetida retorna 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartidoDto cadastrar(@Valid @RequestBody NovoPartido dados) {
        return servico.cadastrarPartido(dados);
    }

    @Operation(summary = "Listar partidos")
    @GetMapping
    public List<PartidoDto> listar() {
        return servico.listarPartidos();
    }
}
