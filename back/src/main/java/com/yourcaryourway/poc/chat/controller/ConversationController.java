package com.yourcaryourway.poc.chat.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yourcaryourway.poc.chat.dto.ConversationResponse;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.service.ChatService;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

  private final ChatService chatService;

  public ConversationController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ConversationResponse create() {
    return chatService.createConversation();
  }

  @GetMapping
  public List<ConversationResponse> findAll() {
    return chatService.findConversations();
  }

  @GetMapping("/{id}/messages")
  public List<MessageResponse> findMessages(@PathVariable UUID id) {
    return chatService.findMessages(id);
  }
}