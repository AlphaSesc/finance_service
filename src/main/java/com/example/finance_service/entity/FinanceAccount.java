package com.example.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finance_accounts")
// Represents a financial account for a student (created via Student Service)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    // Ensures one finance account per student
    private String studentId;

    //denormalized from student service
    private String email;
}