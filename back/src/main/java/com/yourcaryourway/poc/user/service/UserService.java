package com.yourcaryourway.poc.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourcaryourway.poc.user.dto.UserResponse;
import com.yourcaryourway.poc.user.entity.Role;
import com.yourcaryourway.poc.user.repository.UserAccountRepository;

/**
 * Service du module user : accès aux utilisateurs pour les autres modules.
 */
@Service
public class UserService {

  private final UserAccountRepository userRepository;

  public UserService(UserAccountRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Liste les utilisateurs de démonstration.
   *
   * @return tous les utilisateurs
   */
  @Transactional(readOnly = true)
  public List<UserResponse> findAll() {
    return userRepository.findAll().stream().map(UserResponse::from).toList();
  }

  /**
   * Fournit le customer de démonstration.
   * (PoC sans authentification : user qui ouvre toutes les conversations).
   *
   * @return identifiant de l'utilisateur de rôle CUSTOMER
   * @throws IllegalStateException si aucun CUSTOMER n'existe en base
   */
  @Transactional(readOnly = true)
  public UUID getDemoCustomerId() {
    return userRepository.findFirstByRole(Role.CUSTOMER)
        .orElseThrow(() -> new IllegalStateException("Aucun utilisateur CUSTOMER de démonstration en base"))
        .getId();
  }

  /**
   * Indique si un utilisateur existe.
   *
   * @param userId identifiant recherché
   * @return {@code true} si l'utilisateur existe
   */

  @Transactional(readOnly = true)
  public boolean existsById(UUID userId) {
    return userRepository.existsById(userId);
  }
}