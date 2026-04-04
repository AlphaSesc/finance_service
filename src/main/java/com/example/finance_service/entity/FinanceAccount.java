package com.example.finance_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finance_accounts")
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
    private String studentId;

    private String email;
}