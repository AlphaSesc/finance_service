package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request payload for creating a finance account from external services (e.g., Student Service)
public class CreateFinanceAccountRequest {

    private String studentId;
    private String email;
}