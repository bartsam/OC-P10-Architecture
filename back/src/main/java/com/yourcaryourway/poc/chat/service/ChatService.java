package com.yourcaryourway.poc.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.poc.chat.dto.ConversationResponse;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.entity.Conversation;
import com.yourcaryourway.poc.chat.exception.ConversationNotFoundException;
import com.yourcaryourway.poc.chat.repository.ConversationRepository;
import com.yourcaryourway.poc.chat.repository.MessageRepository;
import com.yourcaryourway.poc.user.service.UserService;

@Service
public class ChatService {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final UserService userService;

  public ChatService(
      ConversationRepository conversationRepository,
      MessageRepository messageRepository,
      UserService userService) {
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.userService = userService;
  }

  @Transactional
  public ConversationResponse createConversation() {
    Conversation conversation = new Conversation(userService.getDemoCustomerId());
    return ConversationResponse.from(conversationRepository.save(conversation));
  }

  @Transactional(readOnly = true)
  public List<ConversationResponse> findConversations() {
    return conversationRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(ConversationResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> findMessages(UUID conversationId) {
    if (!conversationRepository.existsById(conversationId)) {
      throw new ConversationNotFoundException(conversationId);
    }
    return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
        .map(MessageResponse::from)
        .toList();
  }
}