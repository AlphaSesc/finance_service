package com.example.finance_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayInvoiceRequest {

    private String reference;
}