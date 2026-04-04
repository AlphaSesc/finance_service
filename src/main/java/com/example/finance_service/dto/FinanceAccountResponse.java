package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceAccountResponse {

    private Long id;
    private String studentId;
    private String email;
}