package br.unirio.eleicao.config;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CriaChefeInicial implements CommandLineRunner {

    private final AutenticacaoServico autenticacao;
    private final String login;
    private final String senha;

    public CriaChefeInicial(AutenticacaoServico autenticacao, AppProperties propriedades) {
        this.autenticacao = autenticacao;
        this.login = propriedades.chefe().login();
        this.senha = propriedades.chefe().senha();
    }

    @Override
    public void run(String... args) {
        autenticacao.garantirChefe(login, senha);
    }
}