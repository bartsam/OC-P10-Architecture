package com.yourcaryourway.poc.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yourcaryourway.poc.chat.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  List<Conversation> findAllByOrderByCreatedAtDesc();
}