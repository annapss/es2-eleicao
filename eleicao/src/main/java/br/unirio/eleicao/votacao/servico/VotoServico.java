package br.unirio.eleicao.votacao.servico;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.eleitor.servico.EleitorServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoId;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoRepository;
import br.unirio.eleicao.votacao.persistencia.Voto;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteId;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import br.unirio.eleicao.votacao.persistencia.VotoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoServico {
    private final AutenticacaoServico autenticacao;
    private final EleitorServico eleitores;
    private final EleicaoServico eleicoes;
    private final CandidatoServico candidatos;
    private final VotoPendenteRepository pendentes;
    private final ComparecimentoRepository comparecimentos;
    private final VotoRepository votos;

    public VotoServico(AutenticacaoServico autenticacao, EleitorServico eleitores,
                       EleicaoServico eleicoes, CandidatoServico candidatos,
                       VotoPendenteRepository pendentes, ComparecimentoRepository comparecimentos,
                       VotoRepository votos) {
        this.autenticacao = autenticacao;
        this.eleitores = eleitores;
        this.eleicoes = eleicoes;
        this.candidatos = candidatos;
        this.pendentes = pendentes;
        this.comparecimentos = comparecimentos;
        this.votos = votos;
    }

    @Transactional
    public void registrar(Long eleicaoId, NovoVoto dados) {
        var usuario = exigirVotacaoPermitida(eleicaoId, dados.cargo());
        CandidatoDto candidato = candidatos.buscar(dados.candidatoId());
        if (!candidato.eleicaoId().equals(eleicaoId) || candidato.cargo() != dados.cargo()) {
            throw new RegraDeNegocioException("Candidato deve pertencer ao cargo e à eleição informados");
        }
        Long cargoId = dados.cargo().getId();
        if (pendentes.inserirSeAusente(usuario.id(), eleicaoId, cargoId, dados.candidatoId()) == 0) {
            throw new ConflitoException("Voto já registrado para este cargo nesta eleição");
        }
    }

    @Transactional
    public void confirmar(Long eleicaoId, Cargo cargo) {
        var usuario = exigirVotacaoPermitida(eleicaoId, cargo);
        Long cargoId = cargo.getId();
        var pendente = pendentes.findById(new VotoPendenteId(usuario.id(), eleicaoId, cargoId))
                .orElseThrow(() -> new NaoEncontradoException("Voto pendente não encontrado para este cargo"));
        if (comparecimentos.inserirSeAusente(usuario.id(), eleicaoId, cargoId) == 0) {
            throw new ConflitoException("Voto já confirmado para este cargo nesta eleição");
        }
        votos.saveAndFlush(new Voto(eleicaoId, cargoId, pendente.candidatoId()));
        pendentes.delete(pendente);
        pendentes.flush();
    }

    private UsuarioAutenticado exigirVotacaoPermitida(Long eleicaoId, Cargo cargo) {
        var usuario = exigirEleitor();
        eleicoes.exigirStatus(eleicaoId, StatusEleicao.ABERTA);
        if (comparecimentos.existsById(new ComparecimentoId(usuario.id(), eleicaoId, cargo.getId()))) {
            throw new ConflitoException("Voto já confirmado para este cargo nesta eleição");
        }
        return usuario;
    }

    private UsuarioAutenticado exigirEleitor() {
        var usuario = autenticacao.usuarioAtual();
        if (usuario.perfil() != Perfil.ELEITOR || !eleitores.existe(usuario.id())) {
            throw new AccessDeniedException("Apenas eleitores cadastrados podem votar");
        }
        return usuario;
    }
}
