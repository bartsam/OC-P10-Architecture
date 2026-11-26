package com.yourcaryourway.poc.chat.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps d'un message envoyé en STOMP sur
 * {@code /app/conversations/{id}/messages}.
 *
 * @param senderId identifiant de l'expéditeur (client ou agent selon la vue)
 * @param content  texte du message, non vide, 2000 caractères au maximum
 */
public record MessageRequest(
      @NotNull UUID senderId,
      @NotBlank @Size(max = 2000) String content) {
}
