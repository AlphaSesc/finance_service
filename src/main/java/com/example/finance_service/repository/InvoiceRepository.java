package com.example.finance_service.repository;

import com.example.finance_service.entity.Invoice;
import com.example.finance_service.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStudentId(String studentId);

    List<Invoice> findByStudentIdAndStatus(String studentId, InvoiceStatus status);

    boolean existsByStudentIdAndStatus(String studentId, InvoiceStatus status);

    Optional<Invoice> findByReference(String reference);
}