package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Used to determine eligibility for actions such as graduation based on unpaid invoices
public class OutstandingBalanceResponse {

    private String studentId;
    private boolean hasOutstandingBalance;
}