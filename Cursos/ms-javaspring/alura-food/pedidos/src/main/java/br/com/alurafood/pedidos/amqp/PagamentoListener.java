package br.com.alurafood.pedidos.amqp;

import br.com.alurafood.pedidos.dto.PagamentoDto;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PagamentoListener {

    @RabbitListener(queues = "pagamento.concluido")
    public void recebeMessagem(PagamentoDto pagamento) {
        String message = """
                Dados de pagamento: %s
                Numero do pedido: %s
                Valor R$: %s
                Status: %s
                """.formatted(pagamento.getId(), pagamento.getPedidoId(),
                pagamento.getValor(), pagamento.getStatus());

        System.out.println("Recebi a mensagem " + message);
    }
}
