package com.yourcaryourway.poc.chat.controller;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.yourcaryourway.poc.chat.dto.MessageRequest;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.service.ChatService;

import jakarta.validation.Valid;

/**
 * Point d'entrée STOMP du tchat : reçoit les messages des clients et les
 * diffuse aux abonnés de la conversation.
 */
@Controller
public class MessageController {

  private final ChatService chatService;

  /**
   * @param chatService service portant la logique du tchat
   */
  public MessageController(ChatService chatService) {
    this.chatService = chatService;
  }

  /**
   * Valide, enregistre et diffuse un message sur
   * {@code /topic/conversations/{id}}
   * uniquement après validation de la transaction
   * garantissant qu'aucun message non persisté n'est diffusé.
   *
   * @param conversationId identifiant de la conversation
   * @param request        contenu du message à valider
   * @return message persisté et diffusé aux abonnés
   */
  @MessageMapping("/conversations/{conversationId}/messages")
  @SendTo("/topic/conversations/{conversationId}")
  public MessageResponse send(
      @DestinationVariable UUID conversationId, @Valid @Payload MessageRequest request) {
    return chatService.sendMessage(conversationId, request);
  }
}