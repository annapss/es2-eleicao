package br.unirio.eleicao.votacao.unit;

import br.unirio.eleicao.shared.Cargo;
import br.unirio.eleicao.votacao.servico.NovoVoto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SigiloVotoTest {
    @Test
    void representacaoDoPedidoEmLogsNaoRevelaEscolha() {
        NovoVoto pedido = new NovoVoto(Cargo.PRESIDENTE, 987654321L);
        String mensagem = "Pedido recebido: " + pedido;
        assertFalse(mensagem.contains("987654321"));
        assertFalse(mensagem.contains("PRESIDENTE"));
        // O dado continua disponivel ao servico para registrar o voto.
        assertEquals(987654321L, pedido.candidatoId());
    }
}
