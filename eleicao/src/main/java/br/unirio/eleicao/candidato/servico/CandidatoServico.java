package br.unirio.eleicao.candidato.servico;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.candidato.persistencia.Candidato;
import br.unirio.eleicao.candidato.persistencia.CandidatoRepository;
import br.unirio.eleicao.candidato.persistencia.Partido;
import br.unirio.eleicao.candidato.persistencia.PartidoRepository;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.StatusEleicao;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CandidatoServico {
    private final CandidatoRepository candidatos;
    private final PartidoRepository partidos;
    private final EleicaoServico eleicao;
    private final AutenticacaoServico autenticacao;

    public CandidatoServico(CandidatoRepository candidatos, PartidoRepository partidos, EleicaoServico eleicao,
                            AutenticacaoServico autenticacao) {
        this.candidatos = candidatos;
        this.partidos = partidos;
        this.eleicao = eleicao;
        this.autenticacao = autenticacao;
    }

    @Transactional
    public CandidatoDto cadastrar(NovoCandidato dados) {
        eleicao.exigirStatus(dados.eleicaoId(), StatusEleicao.AGENDADA);
        exigirPartido(dados.partidoId());
        if (candidatos.contarPorNumero(dados.eleicaoId(), dados.cargo(), dados.numero()) > 0) {
            throw new ConflitoException("Já existe candidato com esse número nesse cargo e eleição");
        }
        Long usuarioId = criarConta(dados);
        Candidato candidato = new Candidato(usuarioId, dados.nome(), dados.numero(), dados.partidoId(),
                dados.cargo(), dados.eleicaoId(), dados.dataNascimento());
        return paraDto(candidatos.save(candidato));
    }

    @Transactional
    public CandidatoDto buscar(Long id) {
        return paraDto(carregar(id));
    }

    @Transactional
    public List<CandidatoDto> listar(Long eleicaoId, Cargo cargo) {
        Map<Long, String> siglas = siglasDosPartidos();
        return candidatos.listarPorFiltro(eleicaoId, cargo).stream()
                .map(candidato -> paraDto(candidato, siglas))
                .toList();
    }

    @Transactional
    public Optional<CandidatoDto> buscarPorUsuario(Long usuarioId, Long eleicaoId) {
        return candidatos.buscarPorUsuarioEEleicao(usuarioId, eleicaoId).map(this::paraDto);
    }

    @Transactional
    public CandidatoDto atualizar(Long id, DadosCandidato dados) {
        Candidato candidato = carregar(id);
        eleicao.exigirStatus(candidato.eleicaoId(), StatusEleicao.AGENDADA);
        exigirPartido(dados.partidoId());
        if (candidatos.contarPorNumeroDeOutroCandidato(candidato.eleicaoId(), dados.cargo(), dados.numero(), id) > 0) {
            throw new ConflitoException("Já existe candidato com esse número nesse cargo e eleição");
        }
        candidato.alterar(dados.nome(), dados.numero(), dados.partidoId(), dados.cargo(), dados.dataNascimento());
        return paraDto(candidato);
    }

    @Transactional
    public void excluir(Long id) {
        Candidato candidato = carregar(id);
        eleicao.exigirStatus(candidato.eleicaoId(), StatusEleicao.AGENDADA);
        try {
            autenticacao.removerConta(candidato.usuarioId());
            candidatos.flush();
        } catch (DataAccessException e) {
            throw new ConflitoException("Candidato que já recebeu votos não pode ser excluído");
        }
    }

    @Transactional
    public PartidoDto cadastrarPartido(NovoPartido dados) {
        if (partidos.contarPorSigla(dados.sigla()) > 0) {
            throw new ConflitoException("Sigla de partido já cadastrada");
        }
        Partido partido = partidos.save(new Partido(dados.sigla(), dados.nome()));
        return new PartidoDto(partido.id(), partido.sigla(), partido.nome());
    }

    @Transactional
    public List<PartidoDto> listarPartidos() {
        return partidos.findAll().stream()
                .map(partido -> new PartidoDto(partido.id(), partido.sigla(), partido.nome()))
                .toList();
    }

    private Long criarConta(NovoCandidato dados) {
        try {
            return autenticacao.criarConta(dados.login(), dados.senha(), Perfil.CANDIDATO);
        } catch (DataAccessException e) {
            throw new ConflitoException("Login já cadastrado");
        }
    }

    private void exigirPartido(Long partidoId) {
        if (!partidos.existsById(partidoId)) {
            throw new NaoEncontradoException("Partido não encontrado");
        }
    }

    private Candidato carregar(Long id) {
        return candidatos.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Candidato não encontrado"));
    }

    private Map<Long, String> siglasDosPartidos() {
        return partidos.findAll().stream().collect(Collectors.toMap(Partido::id, Partido::sigla));
    }

    private CandidatoDto paraDto(Candidato candidato) {
        return paraDto(candidato, siglasDosPartidos());
    }

    private CandidatoDto paraDto(Candidato candidato, Map<Long, String> siglas) {
        return new CandidatoDto(candidato.id(), candidato.nome(), candidato.numero(),
                siglas.get(candidato.partidoId()), candidato.cargo(), candidato.eleicaoId(),
                candidato.dataNascimento());
    }
}
