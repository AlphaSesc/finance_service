package com.example.finance_service.service;

import com.example.finance_service.dto.*;
import com.example.finance_service.entity.FinanceAccount;
import com.example.finance_service.entity.Invoice;
import com.example.finance_service.entity.InvoiceStatus;
import com.example.finance_service.exception.ResourceAlreadyExistsException;
import com.example.finance_service.exception.ResourceNotFoundException;
import com.example.finance_service.repository.FinanceAccountRepository;
import com.example.finance_service.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceAccountRepository financeAccountRepository;
    private final InvoiceRepository invoiceRepository;

    public FinanceAccountResponse createAccount(CreateFinanceAccountRequest request) {
        financeAccountRepository.findByStudentId(request.getStudentId())
                .ifPresent(account -> {
                    throw new ResourceAlreadyExistsException("Finance account already exists for student");
                });

        FinanceAccount account = FinanceAccount.builder()
                .studentId(request.getStudentId())
                .email(request.getEmail())
                .build();

        FinanceAccount savedAccount = financeAccountRepository.save(account);

        return FinanceAccountResponse.builder()
                .id(savedAccount.getId())
                .studentId(savedAccount.getStudentId())
                .email(savedAccount.getEmail())
                .build();
    }

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        financeAccountRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found for student"));

        Invoice invoice = Invoice.builder()
                .studentId(request.getStudentId())
                .courseCode(request.getCourseCode())
                .amount(request.getAmount())
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return InvoiceResponse.builder()
                .id(savedInvoice.getId())
                .studentId(savedInvoice.getStudentId())
                .courseCode(savedInvoice.getCourseCode())
                .amount(savedInvoice.getAmount())
                .status(savedInvoice.getStatus())
                .createdAt(savedInvoice.getCreatedAt())
                .build();
    }

    public OutstandingBalanceResponse checkOutstandingBalance(String studentId) {
        financeAccountRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found for student"));

        boolean hasOutstandingBalance =
                invoiceRepository.existsByStudentIdAndStatus(studentId, InvoiceStatus.PENDING);

        return OutstandingBalanceResponse.builder()
                .studentId(studentId)
                .hasOutstandingBalance(hasOutstandingBalance)
                .build();
    }
}