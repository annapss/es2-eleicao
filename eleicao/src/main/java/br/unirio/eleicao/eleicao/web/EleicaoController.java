package br.unirio.eleicao.eleicao.web;

import br.unirio.eleicao.eleicao.servico.EleicaoDto;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.eleicao.servico.NovaEleicao;
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

@RestController
@RequestMapping("/api/eleicoes")
@Tag(name = "Eleição")
public class EleicaoController {

    private final EleicaoServico servico;

    public EleicaoController(EleicaoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Cadastrar eleição",
            description = "A eleição nasce AGENDADA. Encerramento anterior à abertura retorna 422; data e turno repetidos retornam 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EleicaoDto criar(@Valid @RequestBody NovaEleicao dados) {
        return servico.criar(dados);
    }

    @Operation(summary = "Listar eleições")
    @GetMapping
    public List<EleicaoDto> listar() {
        return servico.listar();
    }

    @Operation(summary = "Consultar a situação de uma eleição",
            description = "Retorna status, data, turno e horários. Eleição inexistente retorna 404.")
    @GetMapping("/{id}")
    public EleicaoDto buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Abrir a votação",
            description = "Só abre eleição AGENDADA; outro status retorna 422. O status passa para ABERTA.")
    @PostMapping("/{id}/abertura")
    public EleicaoDto abrir(@PathVariable Long id) {
        return servico.abrir(id);
    }

    @Operation(summary = "Encerrar a votação",
            description = "Só encerra eleição ABERTA; outro status retorna 422. O status passa para ENCERRADA.")
    @PostMapping("/{id}/encerramento")
    public EleicaoDto encerrar(@PathVariable Long id) {
        return servico.encerrar(id);
    }
}
