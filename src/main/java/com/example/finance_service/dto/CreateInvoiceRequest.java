package com.example.finance_service.dto;

import com.example.finance_service.entity.InvoiceType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {

    private String studentId;
    private String courseCode;
    private BigDecimal amount;
    private InvoiceType invoiceType;
}