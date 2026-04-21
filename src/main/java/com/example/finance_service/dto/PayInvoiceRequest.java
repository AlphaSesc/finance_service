package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO used to process invoice payment using studentId and unique reference
public class PayInvoiceRequest {

    private String studentId;
    private String reference;
}