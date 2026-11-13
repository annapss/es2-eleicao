package br.unirio.eleicao.votacao.servico;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.RegraDeNegocioException;
import br.unirio.eleicao.shared.StatusEleicao;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoId;
import br.unirio.eleicao.votacao.persistencia.ComparecimentoRepository;
import br.unirio.eleicao.votacao.persistencia.VotoPendenteRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoServico {
    private final AutenticacaoServico autenticacao;
    private final EleitorRepository eleitores;
    private final EleicaoServico eleicoes;
    private final CandidatoServico candidatos;
    private final VotoPendenteRepository pendentes;
    private final ComparecimentoRepository comparecimentos;

    public VotoServico(AutenticacaoServico autenticacao, EleitorRepository eleitores,
                       EleicaoServico eleicoes, CandidatoServico candidatos,
                       VotoPendenteRepository pendentes, ComparecimentoRepository comparecimentos) {
        this.autenticacao = autenticacao;
        this.eleitores = eleitores;
        this.eleicoes = eleicoes;
        this.candidatos = candidatos;
        this.pendentes = pendentes;
        this.comparecimentos = comparecimentos;
    }

    @Transactional
    public void registrar(Long eleicaoId, NovoVoto dados) {
        var usuario = autenticacao.usuarioAtual();
        if (usuario.perfil() != Perfil.ELEITOR || !eleitores.existsById(usuario.id())) {
            throw new AccessDeniedException("Apenas eleitores cadastrados podem votar");
        }
        eleicoes.exigirStatus(eleicaoId, StatusEleicao.ABERTA);
        CandidatoDto candidato = candidatos.buscar(dados.candidatoId());
        if (!candidato.eleicaoId().equals(eleicaoId) || candidato.cargo() != dados.cargo()) {
            throw new RegraDeNegocioException("Candidato deve pertencer ao cargo e à eleição informados");
        }
        Long cargoId = dados.cargo().getId();
        if (comparecimentos.existsById(new ComparecimentoId(usuario.id(), eleicaoId, cargoId))) {
            throw new ConflitoException("Voto já confirmado para este cargo nesta eleição");
        }
        // Cada eleição possui seu próprio turno; eleicaoId identifica ambos.
        if (pendentes.inserirSeAusente(usuario.id(), eleicaoId, cargoId, dados.candidatoId()) == 0) {
            throw new ConflitoException("Voto já registrado para este cargo nesta eleição");
        }
    }
}
