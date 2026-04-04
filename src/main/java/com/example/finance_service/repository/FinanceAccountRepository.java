package com.example.finance_service.repository;

import com.example.finance_service.entity.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {

    Optional<FinanceAccount> findByStudentId(String studentId);
}