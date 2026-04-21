package com.example.finance_service.repository;

import com.example.finance_service.entity.Invoice;
import com.example.finance_service.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repository for handling invoice-related database operations
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Checks if a student has any invoice with a given status (e.g., unpaid invoices for graduation check)
    boolean existsByStudentIdAndStatus(String studentId, InvoiceStatus status);

    // Retrieves a specific invoice using studentId and unique reference (used in payment operations)
    Optional<Invoice> findByStudentIdAndReference(String studentId, String reference);

    // Returns all invoices of a student sorted by latest first (used for invoice history display)
    List<Invoice> findByStudentIdOrderByCreatedAtDesc(String studentId);
}