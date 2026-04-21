package com.example.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
// Represents a financial obligation generated from system actions
// (e.g., course enrollment, library fines)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    // Identifies the student associated with the invoice
    private String studentId;

    @Column(name = "course_code")
    // Applicable only for course-related invoices (null for fines, etc.)
    private String courseCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference", nullable = false, unique = true)
    // Unique reference used for tracking and payment operations
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Current state of invoice (e.g., PENDING, PAID)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // Differentiates invoice source (e.g., ENROLLMENT, LIBRARY_FINE)
    private InvoiceType invoiceType;

    @Column(nullable = false)
    //Timestamp when invoice was created
    private LocalDateTime createdAt;
}