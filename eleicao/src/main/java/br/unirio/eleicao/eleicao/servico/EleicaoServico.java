package br.unirio.eleicao.eleicao.servico;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.eleicao.persistencia.Eleicao;
import br.unirio.eleicao.eleicao.persistencia.EleicaoRepository;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class EleicaoServico {
    private final EleicaoRepository repositorio;
    private final AutenticacaoServico autenticacao;

    public EleicaoServico(EleicaoRepository repositorio, AutenticacaoServico autenticacao) {
        this.repositorio = repositorio;
        this.autenticacao = autenticacao;
    }

    @Transactional
    public EleicaoDto criar(NovaEleicao dados) {
        if (!dados.horaEncerramento().isAfter(dados.horaAbertura())) {
            throw new RegraDeNegocioException("O horário de encerramento deve ser posterior ao de abertura");
        }
        if (repositorio.contarPorDataETurno(dados.data(), dados.turno()) > 0) {
            throw new ConflitoException("Já existe eleição nessa data e turno");
        }
        Eleicao eleicao = new Eleicao(dados.data(), dados.turno(), dados.horaAbertura(),
                dados.horaEncerramento(), autenticacao.usuarioAtual().id());
        return paraDto(repositorio.save(eleicao));
    }

    @Transactional
    public EleicaoDto buscar(Long id) {
        return paraDto(carregar(id));
    }

    @Transactional
    public List<EleicaoDto> listar() {
        return repositorio.findAll().stream().map(this::paraDto).toList();
    }

    @Transactional
    public EleicaoDto abrir(Long id) {
        Eleicao eleicao = carregar(id);
        exigir(eleicao, StatusEleicao.AGENDADA);
        eleicao.registrarAbertura(agora());
        return paraDto(eleicao);
    }

    @Transactional
    public EleicaoDto encerrar(Long id) {
        Eleicao eleicao = carregar(id);
        exigir(eleicao, StatusEleicao.ABERTA);
        eleicao.registrarEncerramento(agora());
        return paraDto(eleicao);
    }

    @Transactional
    public void exigirStatus(Long eleicaoId, StatusEleicao esperado) {
        exigir(carregar(eleicaoId), esperado);
    }

    private void exigir(Eleicao eleicao, StatusEleicao esperado) {
        if (eleicao.status() != esperado) {
            throw new RegraDeNegocioException(
                    "A eleição precisa estar " + esperado + ", mas está " + eleicao.status());
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private Eleicao carregar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Eleição não encontrada"));
    }

    private EleicaoDto paraDto(Eleicao eleicao) {
        return new EleicaoDto(eleicao.id(), eleicao.dataEleicao(), eleicao.turno(),
                eleicao.horaAbertura(), eleicao.horaEncerramento(), eleicao.status());
    }
}
