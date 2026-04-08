package com.example.finance_service.dto;

import com.example.finance_service.entity.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private Long id;
    private String studentId;
    private String courseCode;
    private BigDecimal amount;
    private String reference;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}