package br.unirio.eleicao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Chefe chefe) {
    public record Jwt(String secret, long validadeMinutos) {}

    public record Chefe(String login, String senha) {}
}
