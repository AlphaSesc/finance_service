package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutstandingBalanceResponse {

    private String studentId;
    private boolean hasOutstandingBalance;
}