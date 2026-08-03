package com.biscoitos.manutencao.paradas.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT (não @EventListener simples) de propósito: se a transação do
 * ParadaService.abrirParada/encerrarParada der rollback por qualquer motivo depois
 * do evento ser publicado, não queremos ter notificado o dashboard sobre uma parada
 * que na prática não foi persistida.
 */
@Component
@RequiredArgsConstructor
public class ParadaEventoListener {

    private static final String TOPICO = "/topic/paradas";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoRegistrarEvento(ParadaEventoTempoReal evento) {
        messagingTemplate.convertAndSend(TOPICO, evento);
    }
}
