package com.yourcaryourway.poc.chat.exception;

import java.util.UUID;

/**
 * Levée lorsqu'un message est envoyé par un utilisateur qui n'existe pas.
 */
public class UnknownSenderException extends RuntimeException {

  /**
   * @param senderId identifiant de l'expéditeur introuvable
   */
  public UnknownSenderException(UUID senderId) {
    super("Expéditeur inconnu : " + senderId);
  }
}