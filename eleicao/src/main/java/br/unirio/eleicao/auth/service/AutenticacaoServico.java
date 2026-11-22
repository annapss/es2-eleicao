package br.unirio.eleicao.auth.service;

import br.unirio.eleicao.auth.repository.Usuario;
import br.unirio.eleicao.auth.repository.UsuarioRepository;
import br.unirio.eleicao.shared.CredenciaisInvalidasException;
import br.unirio.eleicao.shared.Perfil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoServico {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final TokenJwt jwt;

    public AutenticacaoServico(UsuarioRepository repo, PasswordEncoder encoder, TokenJwt jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public String login(String login, String senha) {
        Usuario u = repo.findByLoginAndAtivoTrue(login)
            .filter(x -> encoder.matches(senha, x.senhaHash()))
            .orElseThrow(CredenciaisInvalidasException::new);
        return jwt.gerar(u.id(), u.perfil());
    }

    @Transactional
    public Long criarConta(String login, String senha, Perfil perfil) {
        return repo.save(new Usuario(login, encoder.encode(senha), perfil)).id();
    }

    @Transactional
    public void removerConta(Long id) { repo.deleteById(id); }

    public UsuarioAutenticado usuarioAtual() {
        return (UsuarioAutenticado) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
    
    @Transactional
    public void garantirChefe(String login, String senha) {
        if (repo.existsByLogin(login)) return;
        criarConta(login, senha, Perfil.CHEFE_SECAO);
    }
}