package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.apuracao.persistencia.ApuracaoCandidato;
import br.unirio.eleicao.apuracao.persistencia.ApuracaoRepository;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.StatusEleicao;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApuracaoServico {
    private final ApuracaoRepository apuracao;
    private final EleicaoServico eleicao;
    private final AutenticacaoServico autenticacao;

    public ApuracaoServico(ApuracaoRepository apuracao, EleicaoServico eleicao, AutenticacaoServico autenticacao) {
        this.apuracao = apuracao;
        this.eleicao = eleicao;
        this.autenticacao = autenticacao;
    }

    // Placar parcial da eleição ABERTA: votos confirmados por candidato, de todos os cargos
    // ou de um cargo específico. É calculado a cada consulta, então sempre reflete os votos atuais.
    @Transactional(readOnly = true)
    public List<LinhaApuracao> parcial(Long eleicaoId, Cargo cargo) {
        if (autenticacao.usuarioAtual().perfil() == Perfil.CANDIDATO) {
            // O acompanhamento restrito ao próprio cargo do candidato é a US-22.
            throw new AccessDeniedException("Acompanhamento do candidato ainda não disponível");
        }
        eleicao.exigirStatus(eleicaoId, StatusEleicao.ABERTA);
        return apuracao.listar(eleicaoId, cargo).stream().map(this::paraDto).toList();
    }

    private LinhaApuracao paraDto(ApuracaoCandidato linha) {
        return new LinhaApuracao(linha.cargo(), linha.candidatoId(), linha.numero(), linha.nome(),
                linha.partido(), linha.totalVotos());
    }
}
