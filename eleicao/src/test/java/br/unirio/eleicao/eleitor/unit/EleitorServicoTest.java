package br.unirio.eleicao.eleitor.unit;

import br.unirio.eleicao.auth.service.AutenticacaoServico;
import br.unirio.eleicao.eleitor.persistencia.Eleitor;
import br.unirio.eleicao.eleitor.persistencia.EleitorRepository;
import br.unirio.eleicao.eleitor.servico.DadosEleitor;
import br.unirio.eleicao.eleitor.servico.EleitorDto;
import br.unirio.eleicao.eleitor.servico.EleitorServico;
import br.unirio.eleicao.eleitor.servico.NovoEleitor;
import br.unirio.eleicao.shared.ConflitoException;
import br.unirio.eleicao.shared.NaoEncontradoException;
import br.unirio.eleicao.shared.Perfil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleitorServicoTest {
    private static final String CPF = "12345678901";
    private static final String TITULO = "123456789012";
    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 5, 20);

    @Mock
    private EleitorRepository repositorio;

    @Mock
    private AutenticacaoServico autenticacao;

    @InjectMocks
    private EleitorServico servico;

    @Test
    void cadastrarCriaContaDeEleitorEUsaOIdDela() {
        when(autenticacao.criarConta("ana", "senha", Perfil.ELEITOR)).thenReturn(5L);
        when(repositorio.save(any(Eleitor.class))).thenAnswer(chamada -> chamada.getArgument(0));

        EleitorDto dto = servico.cadastrar(novoEleitor());

        assertEquals(5L, dto.id());
        assertEquals(CPF, dto.cpf());
        assertEquals(NASCIMENTO, dto.dataNascimento());
    }

    @Test
    void cadastrarRecusaCpfRepetidoSemCriarConta() {
        when(repositorio.contarPorCpf(CPF)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.cadastrar(novoEleitor()));
        verify(autenticacao, never()).criarConta(anyString(), anyString(), any());
    }

    @Test
    void cadastrarRecusaTituloRepetidoSemCriarConta() {
        when(repositorio.contarPorTitulo(TITULO)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.cadastrar(novoEleitor()));
        verify(autenticacao, never()).criarConta(anyString(), anyString(), any());
    }

    @Test
    void cadastrarConverteLoginRepetidoEmConflito() {
        when(autenticacao.criarConta("ana", "senha", Perfil.ELEITOR))
                .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed"));

        assertThrows(ConflitoException.class, () -> servico.cadastrar(novoEleitor()));
    }

    @Test
    void buscarDevolveEleitor() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(eleitor()));

        assertEquals("Ana", servico.buscar(5L).nome());
    }

    @Test
    void buscarEleitorInexistenteLancaNaoEncontrado() {
        when(repositorio.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.buscar(5L));
    }

    @Test
    void listarDevolveTodosOsEleitores() {
        when(repositorio.findAll()).thenReturn(List.of(eleitor()));

        assertEquals(1, servico.listar().size());
    }

    @Test
    void atualizarAlteraOsDados() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(eleitor()));

        EleitorDto dto = servico.atualizar(5L, dadosEleitor("Ana Maria"));

        assertEquals("Ana Maria", dto.nome());
    }

    @Test
    void atualizarEleitorInexistenteLancaNaoEncontrado() {
        when(repositorio.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> servico.atualizar(5L, dadosEleitor("Ana")));
    }

    @Test
    void atualizarRecusaCpfDeOutroEleitor() {
        Eleitor eleitor = eleitor();
        when(repositorio.findById(5L)).thenReturn(Optional.of(eleitor));
        when(repositorio.contarPorCpfDeOutroEleitor(CPF, 5L)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.atualizar(5L, dadosEleitor("Outro")));
        assertEquals("Ana", eleitor.nome());
    }

    @Test
    void atualizarRecusaTituloDeOutroEleitor() {
        Eleitor eleitor = eleitor();
        when(repositorio.findById(5L)).thenReturn(Optional.of(eleitor));
        when(repositorio.contarPorTituloDeOutroEleitor(TITULO, 5L)).thenReturn(1L);

        assertThrows(ConflitoException.class, () -> servico.atualizar(5L, dadosEleitor("Outro")));
        assertEquals("Ana", eleitor.nome());
    }

    @Test
    void excluirRemoveAContaEForcaAEscritaNoBanco() {
        when(repositorio.existsById(5L)).thenReturn(true);

        servico.excluir(5L);

        verify(autenticacao).removerConta(5L);
        verify(repositorio).flush();
    }

    @Test
    void excluirEleitorInexistenteLancaNaoEncontrado() {
        when(repositorio.existsById(5L)).thenReturn(false);

        assertThrows(NaoEncontradoException.class, () -> servico.excluir(5L));
        verify(autenticacao, never()).removerConta(anyLong());
    }

    @Test
    void excluirConverteFalhaDoBancoEmConflito() {
        when(repositorio.existsById(5L)).thenReturn(true);
        doThrow(new JpaSystemException(new RuntimeException("FOREIGN KEY constraint failed")))
                .when(repositorio).flush();

        assertThrows(ConflitoException.class, () -> servico.excluir(5L));
    }

    private NovoEleitor novoEleitor() {
        return new NovoEleitor("Ana", CPF, TITULO, NASCIMENTO, "ana", "senha");
    }

    private DadosEleitor dadosEleitor(String nome) {
        return new DadosEleitor(nome, CPF, TITULO, NASCIMENTO);
    }

    private Eleitor eleitor() {
        return new Eleitor(5L, "Ana", CPF, TITULO, NASCIMENTO);
    }
}
