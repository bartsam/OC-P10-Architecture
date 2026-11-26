package com.yourcaryourway.poc.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.yourcaryourway.poc.chat.entity.Conversation;

public record ConversationResponse(UUID id, UUID userId, Instant createdAt) {

  public static ConversationResponse from(Conversation conversation) {
    return new ConversationResponse(conversation.getId(), conversation.getUserId(), conversation.getCreatedAt());
  }
}