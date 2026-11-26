package com.yourcaryourway.poc.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.poc.user.dto.UserResponse;
import com.yourcaryourway.poc.user.entity.Role;
import com.yourcaryourway.poc.user.repository.UserAccountRepository;

@Service
public class UserService {

  private final UserAccountRepository userRepository;

  public UserService(UserAccountRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<UserResponse> findAll() {
    return userRepository.findAll().stream().map(UserResponse::from).toList();
  }

  /**
   * PoC sans authentification : le client de démonstration ouvre toutes les
   * conversations.
   */
  @Transactional(readOnly = true)
  public UUID getDemoCustomerId() {
    return userRepository.findFirstByRole(Role.CUSTOMER)
        .orElseThrow(() -> new IllegalStateException("Aucun utilisateur CUSTOMER de démonstration en base"))
        .getId();
  }
}