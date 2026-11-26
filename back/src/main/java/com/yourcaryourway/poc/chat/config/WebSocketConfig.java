package com.yourcaryourway.poc.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration du WebSocket et du protocole STOMP utilisés par le tchat.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  /**
   * Déclare le point d'entrée WebSocket natif {@code /ws}
   *
   * @param registry registre des points d'entrée STOMP
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws");
  }

  /**
   * Configure le routage des messages, les clients :
   * - envoient vers {@code /app/...}
   * - s'abonnent à {@code /topic/...}
   * Le broker est celui, en mémoire, de Spring (PoC : une instance sans Redis).
   *
   * @param registry registre du broker de messages
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.enableSimpleBroker("/topic");
  }
}