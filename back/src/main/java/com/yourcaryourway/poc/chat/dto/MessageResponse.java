package com.yourcaryourway.poc.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.yourcaryourway.poc.chat.entity.Message;

public record MessageResponse(UUID id, UUID conversationId, UUID senderId, String content, Instant sentAt) {

  public static MessageResponse from(Message message) {
    return new MessageResponse(message.getId(), message.getConversationId(), message.getSenderId(),
        message.getContent(), message.getSentAt());
  }
}