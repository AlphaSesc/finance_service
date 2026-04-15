package com.example.finance_service.service;

import com.example.finance_service.dto.*;
import com.example.finance_service.entity.FinanceAccount;
import com.example.finance_service.entity.Invoice;
import com.example.finance_service.entity.InvoiceStatus;
import com.example.finance_service.entity.InvoiceType;
import com.example.finance_service.exception.InvalidRequestException;
import com.example.finance_service.exception.ResourceAlreadyExistsException;
import com.example.finance_service.exception.ResourceNotFoundException;
import com.example.finance_service.repository.FinanceAccountRepository;
import com.example.finance_service.repository.InvoiceRepository;
import com.example.finance_service.util.InvoiceReferenceGenerator;
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

        validateCreateInvoiceRequest(request);

        Invoice invoice = Invoice.builder()
                .studentId(request.getStudentId())
                .courseCode(request.getCourseCode())
                .amount(request.getAmount())
                .invoiceType(request.getInvoiceType())
                .reference(InvoiceReferenceGenerator.generate())
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return mapToInvoiceResponse(savedInvoice);
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

    public InvoiceResponse payInvoice(PayInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByReference(request.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResourceAlreadyExistsException("Invoice is already paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        return mapToInvoiceResponse(updatedInvoice);
    }

    private void validateCreateInvoiceRequest(CreateInvoiceRequest request) {
        if (request.getInvoiceType() == null) {
            throw new InvalidRequestException("Invoice type is required");
        }

        if (request.getInvoiceType() == InvoiceType.COURSE_ENROLLMENT) {
            if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
                throw new InvalidRequestException("Course code is required for course enrollment invoice");
            }
        }

        if (request.getInvoiceType() == InvoiceType.LIBRARY_FINE) {
            if (request.getCourseCode() != null && !request.getCourseCode().isBlank()) {
                throw new InvalidRequestException("Course code must not be provided for library fine invoice");
            }
        }
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .studentId(invoice.getStudentId())
                .courseCode(invoice.getCourseCode())
                .invoiceType(invoice.getInvoiceType())
                .amount(invoice.getAmount())
                .reference(invoice.getReference())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }


}