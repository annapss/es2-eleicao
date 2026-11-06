package br.unirio.eleicao.auth.service;

import br.unirio.eleicao.config.AppProperties;
import br.unirio.eleicao.shared.Perfil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
class TokenJwt {
    private final SecretKey chave; private final long minutos;
    TokenJwt(AppProperties propriedades) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(propriedades.jwt().secret()));
        this.minutos = propriedades.jwt().validadeMinutos();
    }
    String gerar(Long id, Perfil perfil) {
        Instant agora = Instant.now();
        return Jwts.builder().subject(id.toString()).claim("perfil", perfil.name())
            .issuedAt(Date.from(agora)).expiration(Date.from(agora.plus(minutos, ChronoUnit.MINUTES)))
            .signWith(chave).compact();
    }
    UsuarioAutenticado validar(String token) {
        Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        return new UsuarioAutenticado(Long.valueOf(c.getSubject()), Perfil.valueOf(c.get("perfil", String.class)));
    }
}