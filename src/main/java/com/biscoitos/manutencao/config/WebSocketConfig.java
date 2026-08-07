package com.biscoitos.manutencao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * T04. Canal complementar ao REST — usado só para notificação em tempo real
 * (ex.: dashboard/supervisor vendo uma parada aparecer sem dar refresh), nunca
 * para operações de escrita (essas continuam sendo REST, conforme decidido no
 * discovery/ADR-001).
 *
 * "/topic/paradas" é o único tópico por ora — qualquer parada aberta ou encerrada
 * é publicada nele. Um tópico por equipamento (ex. "/topic/paradas/{equipamentoId}")
 * é uma evolução natural se a lista de assinantes crescer, mas não se justifica ainda.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
