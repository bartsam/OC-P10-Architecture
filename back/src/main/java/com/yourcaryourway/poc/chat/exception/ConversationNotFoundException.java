package com.yourcaryourway.poc.chat.exception;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {

  public ConversationNotFoundException(UUID conversationId) {
    super("Conversation introuvable : " + conversationId);
  }
}