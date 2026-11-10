package br.unirio.eleicao.eleitor.servico;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.eleitor.persistencia.Eleitor;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EleitorServico {
    private final EleitorRepository repositorio;
    private final AutenticacaoServico autenticacao;

    public EleitorServico(EleitorRepository repositorio, AutenticacaoServico autenticacao) {
        this.repositorio = repositorio;
        this.autenticacao = autenticacao;
    }

    @Transactional
    public EleitorDto cadastrar(NovoEleitor dados) {
        if (repositorio.contarPorCpf(dados.cpf()) > 0) {
            throw new ConflitoException("CPF já cadastrado");
        }
        if (repositorio.contarPorTitulo(dados.tituloEleitor()) > 0) {
            throw new ConflitoException("Título de eleitor já cadastrado");
        }
        Long usuarioId = criarConta(dados);
        Eleitor eleitor = new Eleitor(usuarioId, dados.nome(), dados.cpf(), dados.tituloEleitor(),
                dados.dataNascimento());
        return paraDto(repositorio.save(eleitor));
    }

    @Transactional
    public EleitorDto buscar(Long id) {
        return paraDto(carregar(id));
    }

    @Transactional
    public List<EleitorDto> listar() {
        return repositorio.findAll().stream().map(this::paraDto).toList();
    }

    @Transactional
    public EleitorDto atualizar(Long id, DadosEleitor dados) {
        Eleitor eleitor = carregar(id);
        if (repositorio.contarPorCpfDeOutroEleitor(dados.cpf(), id) > 0) {
            throw new ConflitoException("CPF já cadastrado");
        }
        if (repositorio.contarPorTituloDeOutroEleitor(dados.tituloEleitor(), id) > 0) {
            throw new ConflitoException("Título de eleitor já cadastrado");
        }
        eleitor.alterar(dados.nome(), dados.cpf(), dados.tituloEleitor(), dados.dataNascimento());
        return paraDto(eleitor);
    }

    @Transactional
    public void excluir(Long id) {
        if (!repositorio.existsById(id)) {
            throw new NaoEncontradoException("Eleitor não encontrado");
        }
        try {
            autenticacao.removerConta(id);
            repositorio.flush();
        } catch (DataAccessException e) {
            throw new ConflitoException("Eleitor que já votou não pode ser excluído");
        }
    }

    private Long criarConta(NovoEleitor dados) {
        try {
            return autenticacao.criarConta(dados.login(), dados.senha(), Perfil.ELEITOR);
        } catch (DataAccessException e) {
            throw new ConflitoException("Login já cadastrado");
        }
    }

    private Eleitor carregar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Eleitor não encontrado"));
    }

    private EleitorDto paraDto(Eleitor eleitor) {
        return new EleitorDto(eleitor.usuarioId(), eleitor.nome(), eleitor.cpf(), eleitor.tituloEleitor(),
                eleitor.dataNascimento());
    }
}
