package com.yourcaryourway.poc.chat.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions du module chat en réponses REST
 */
@RestControllerAdvice
public class ChatExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ChatExceptionHandler.class);

  /**
   * Répond 404 lorsqu'une conversation demandée en REST n'existe pas.
   *
   * @param ex exception levée par le service
   * @return détail de l'erreur au format {@code application/problem+json}
   */
  @ExceptionHandler(ConversationNotFoundException.class)
  public ProblemDetail handleNotFound(ConversationNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /**
   * Journalise message STOMP refusé (invalide, conversation/expéditeur inconnu)
   * Message ni enregistré ni diffusé, et aucune réponse n'est envoyée
   *
   * @param ex cause du refus
   */
  @MessageExceptionHandler({
      MethodArgumentNotValidException.class,
      ConversationNotFoundException.class,
      UnknownSenderException.class })
  public void handleRejectedMessage(Exception ex) {
    log.warn("Message STOMP refusé : {}", ex.getMessage());
  }
}