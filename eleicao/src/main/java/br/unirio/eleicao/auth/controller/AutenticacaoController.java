package br.unirio.eleicao.auth.controller;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AutenticacaoController {

    private final AutenticacaoServico servico;

    public AutenticacaoController(AutenticacaoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Login", description = "Retorna o token JWT. Credenciais inválidas retornam 401.")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        return new TokenResponse(servico.login(r.login(), r.senha()));
    }
}