package com.yourcaryourway.poc.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yourcaryourway.poc.user.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
}