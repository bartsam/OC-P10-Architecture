package com.yourcaryourway.poc.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.poc.chat.dto.ConversationResponse;
import com.yourcaryourway.poc.chat.dto.MessageRequest;
import com.yourcaryourway.poc.chat.dto.MessageResponse;
import com.yourcaryourway.poc.chat.entity.Conversation;
import com.yourcaryourway.poc.chat.entity.Message;
import com.yourcaryourway.poc.chat.exception.ConversationNotFoundException;
import com.yourcaryourway.poc.chat.exception.UnknownSenderException;
import com.yourcaryourway.poc.chat.repository.ConversationRepository;
import com.yourcaryourway.poc.chat.repository.MessageRepository;
import com.yourcaryourway.poc.user.service.UserService;

/**
 * Logique du tchat : conversations, historique et enregistrement des messages.
 */
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

  /**
   * Crée une conversation (ouverte par le customer demo).
   *
   * @return conversation créée
   */
  @Transactional
  public ConversationResponse createConversation() {
    Conversation conversation = new Conversation(userService.getDemoCustomerId());
    return ConversationResponse.from(conversationRepository.save(conversation));
  }

  /**
   * Liste les conversations, de la plus récente à la plus ancienne.
   *
   * @return les conversations existantes
   */
  @Transactional(readOnly = true)
  public List<ConversationResponse> findConversations() {
    return conversationRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(ConversationResponse::from)
        .toList();
  }

  /**
   * Lit l'historique d'une conversation, trié par date d'envoi croissante.
   *
   * @param conversationId identifiant de la conversation
   * @return les messages, du plus ancien au plus récent
   * @throws ConversationNotFoundException si la conversation n'existe pas
   */
  @Transactional(readOnly = true)
  public List<MessageResponse> findMessages(UUID conversationId) {
    requireConversation(conversationId);
    return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
        .map(MessageResponse::from)
        .toList();
  }

  /**
   * Vérifie la conversation et l'expéditeur, enregistre le message
   * (sent_at fixé par le serveur), puis renvoie le message à diffuser.
   *
   * @param conversationId identifiant de la conversation
   * @param request        contenu et expéditeur du message
   * @return le message enregistré
   * @throws ConversationNotFoundException si la conversation n'existe pas
   * @throws UnknownSenderException        si l'expéditeur n'existe pas
   */
  @Transactional
  public MessageResponse sendMessage(UUID conversationId, MessageRequest request) {
    requireConversation(conversationId);
    if (!userService.existsById(request.senderId())) {
      throw new UnknownSenderException(request.senderId());
    }
    Message message = new Message(conversationId, request.senderId(), request.content());
    return MessageResponse.from(messageRepository.save(message));
  }

  /**
   * Vérifie qu'une conversation existe.
   *
   * @param conversationId identifiant à vérifier
   * @throws ConversationNotFoundException si elle n'existe pas
   */
  private void requireConversation(UUID conversationId) {
    if (!conversationRepository.existsById(conversationId)) {
      throw new ConversationNotFoundException(conversationId);
    }
  }
}