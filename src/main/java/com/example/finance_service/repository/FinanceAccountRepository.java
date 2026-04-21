package com.example.finance_service.repository;

import com.example.finance_service.entity.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository for performing database operations on FinanceAccount entity
public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {

    // Retrieves finance account using studentId (used for cross-service lookups)
    Optional<FinanceAccount> findByStudentId(String studentId);
}