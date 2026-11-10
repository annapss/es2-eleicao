package br.unirio.eleicao.eleitor.web;

import br.unirio.eleicao.eleitor.servico.DadosEleitor;
import br.unirio.eleicao.eleitor.servico.EleitorDto;
import br.unirio.eleicao.eleitor.servico.EleitorServico;
import br.unirio.eleicao.eleitor.servico.NovoEleitor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eleitores")
@Tag(name = "Eleitores")
public class EleitorController {

    private final EleitorServico servico;

    public EleitorController(EleitorServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Cadastrar eleitor",
            description = "Salva os dados e cria a conta de login. CPF, título ou login repetidos retornam 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EleitorDto cadastrar(@Valid @RequestBody NovoEleitor dados) {
        return servico.cadastrar(dados);
    }

    @Operation(summary = "Listar eleitores")
    @GetMapping
    public List<EleitorDto> listar() {
        return servico.listar();
    }

    @Operation(summary = "Buscar eleitor por id", description = "Eleitor inexistente retorna 404.")
    @GetMapping("/{id}")
    public EleitorDto buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @Operation(summary = "Alterar dados do eleitor",
            description = "Não altera login nem senha. Eleitor inexistente retorna 404; CPF ou título de outro eleitor retornam 409.")
    @PutMapping("/{id}")
    public EleitorDto atualizar(@PathVariable Long id, @Valid @RequestBody DadosEleitor dados) {
        return servico.atualizar(id, dados);
    }

    @Operation(summary = "Excluir eleitor",
            description = "Remove o eleitor e a conta de login. Eleitor inexistente retorna 404; eleitor que já votou retorna 409.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        servico.excluir(id);
    }
}
