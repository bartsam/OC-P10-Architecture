package com.yourcaryourway.poc.user.dto;

import java.util.UUID;

import com.yourcaryourway.poc.user.entity.Role;
import com.yourcaryourway.poc.user.entity.UserAccount;

public record UserResponse(UUID id, String firstName, String lastName, Role role) {

  public static UserResponse from(UserAccount user) {
    return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getRole());
  }
}