package br.unirio.eleicao.apuracao.servico;

import br.unirio.eleicao.apuracao.persistencia.ApuracaoCandidato;
import br.unirio.eleicao.apuracao.persistencia.ApuracaoRepository;
import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.auth.service.UsuarioAutenticado;
import br.unirio.eleicao.candidato.servico.CandidatoDto;
import br.unirio.eleicao.candidato.servico.CandidatoServico;
import br.unirio.eleicao.eleicao.servico.EleicaoServico;
import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.shared.Perfil;
import br.unirio.eleicao.shared.StatusEleicao;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ApuracaoServico {
    private final ApuracaoRepository apuracao;
    private final EleicaoServico eleicao;
    private final CandidatoServico candidatos;
    private final AutenticacaoServico autenticacao;

    public ApuracaoServico(ApuracaoRepository apuracao, EleicaoServico eleicao, CandidatoServico candidatos,
                           AutenticacaoServico autenticacao) {
        this.apuracao = apuracao;
        this.eleicao = eleicao;
        this.candidatos = candidatos;
        this.autenticacao = autenticacao;
    }

    // Placar parcial da eleição ABERTA: votos confirmados por candidato, de todos os cargos
    // ou de um cargo específico. É calculado a cada consulta, então sempre reflete os votos atuais.
    // Eleitor e Chefe da Seção veem qualquer cargo; o candidato vê apenas o cargo que disputa.
    @Transactional(readOnly = true)
    public List<LinhaApuracao> parcial(Long eleicaoId, Cargo cargo) {
        UsuarioAutenticado usuario = autenticacao.usuarioAtual();
        eleicao.exigirStatus(eleicaoId, StatusEleicao.ABERTA);
        Cargo filtro = usuario.perfil() == Perfil.CANDIDATO ? cargoDoCandidato(usuario, eleicaoId, cargo) : cargo;
        return apuracao.listar(eleicaoId, filtro).stream().map(this::paraDto).toList();
    }

    // Resultado final, disponível apenas com a eleição ENCERRADA. Todos os perfis veem todos os
    // cargos: a restrição do candidato vale só para o acompanhamento durante a votação.
    // Todos os cargos aparecem, mesmo sem candidatos (lista vazia).
    @Transactional(readOnly = true)
    public ResultadoFinal resultado(Long eleicaoId) {
        eleicao.exigirStatus(eleicaoId, StatusEleicao.ENCERRADA);
        Map<Cargo, List<LinhaApuracao>> porCargo = new EnumMap<>(Cargo.class);
        for (Cargo cargo : Cargo.values()) {
            porCargo.put(cargo, new ArrayList<>());
        }
        for (ApuracaoCandidato linha : apuracao.listar(eleicaoId, null)) {
            porCargo.get(linha.cargo()).add(paraDto(linha));
        }
        List<ResultadoCargo> cargos = porCargo.entrySet().stream()
                .map(item -> new ResultadoCargo(item.getKey(), List.copyOf(item.getValue())))
                .toList();
        return new ResultadoFinal(eleicaoId, cargos);
    }

    // Sem filtro, o candidato recebe o próprio cargo. Pedir outro cargo, ou consultar uma eleição
    // em que não concorre, retorna 403.
    private Cargo cargoDoCandidato(UsuarioAutenticado usuario, Long eleicaoId, Cargo pedido) {
        Cargo proprio = candidatos.buscarPorUsuario(usuario.id(), eleicaoId)
                .map(CandidatoDto::cargo)
                .orElseThrow(() -> new AccessDeniedException("Candidato não concorre nesta eleição"));
        if (pedido != null && pedido != proprio) {
            throw new AccessDeniedException("Candidato só acompanha o cargo que disputa");
        }
        return proprio;
    }

    private LinhaApuracao paraDto(ApuracaoCandidato linha) {
        return new LinhaApuracao(linha.cargo(), linha.candidatoId(), linha.numero(), linha.nome(),
                linha.partido(), linha.totalVotos());
    }
}
