package com.yourcaryourway.poc.chat.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "message")
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "conversation_id", nullable = false)
  private UUID conversationId;

  /** Expéditeur : client ou agent (identifiant du module user). */
  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  /** Fixé par le serveur à la création, jamais par le client. */
  @Column(name = "sent_at", nullable = false)
  private Instant sentAt;

  protected Message() {
    // requis par JPA
  }

  public Message(UUID conversationId, UUID senderId, String content) {
    this.conversationId = conversationId;
    this.senderId = senderId;
    this.content = content;
    this.sentAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getConversationId() {
    return conversationId;
  }

  public UUID getSenderId() {
    return senderId;
  }

  public String getContent() {
    return content;
  }

  public Instant getSentAt() {
    return sentAt;
  }
}