package com.yourcaryourway.poc.chat.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yourcaryourway.poc.chat.entity.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {
}