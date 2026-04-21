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
import java.util.List;

@Service
@RequiredArgsConstructor
// Core service responsible for handling finance-related business logic
// including account management, invoice generation, and payment processing
public class FinanceService {

    private final FinanceAccountRepository financeAccountRepository;
    private final InvoiceRepository invoiceRepository;

    public FinanceAccountResponse createAccount(CreateFinanceAccountRequest request) {

        // Prevent duplicate finance accounts for the same student
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

        // Ensure student has a finance account before creating invoice
        financeAccountRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found for student"));

        // Validate request based on invoice type rules
        validateCreateInvoiceRequest(request);

        Invoice invoice = Invoice.builder()
                .studentId(request.getStudentId())
                .courseCode(request.getCourseCode())
                .amount(request.getAmount())
                .invoiceType(request.getInvoiceType())
                // Generate unique reference for tracking and payment
                .reference(InvoiceReferenceGenerator.generate())
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        return mapToInvoiceResponse(savedInvoice);
    }

    public OutstandingBalanceResponse checkOutstandingBalance(String studentId) {

        // Validate existence of finance account
        financeAccountRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found for student"));

        // Check for any unpaid invoices (used for graduation eligibility)
        boolean hasOutstandingBalance =
                invoiceRepository.existsByStudentIdAndStatus(studentId, InvoiceStatus.PENDING);

        return OutstandingBalanceResponse.builder()
                .studentId(studentId)
                .hasOutstandingBalance(hasOutstandingBalance)
                .build();
    }

    public InvoiceResponse payInvoice(PayInvoiceRequest request) {

        // Retrieve invoice using studentId + reference for secure lookup
        Invoice invoice = invoiceRepository.findByStudentIdAndReference(request.getStudentId(), request.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Prevent duplicate payments
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResourceAlreadyExistsException("Invoice is already paid");
        }

        //Marks invoice as paid
        invoice.setStatus(InvoiceStatus.PAID);

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        return mapToInvoiceResponse(updatedInvoice);
    }

    private void validateCreateInvoiceRequest(CreateInvoiceRequest request) {

        // Invoice type must be provided
        if (request.getInvoiceType() == null) {
            throw new InvalidRequestException("Invoice type is required");
        }

        // Course enrollment invoices must include courseCode
        if (request.getInvoiceType() == InvoiceType.COURSE_ENROLLMENT) {
            if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
                throw new InvalidRequestException("Course code is required for course enrollment invoice");
            }
        }

        //Library fines must not include courseCode
        if (request.getInvoiceType() == InvoiceType.LIBRARY_FINE) {
            if (request.getCourseCode() != null && !request.getCourseCode().isBlank()) {
                throw new InvalidRequestException("Course code must not be provided for library fine invoice");
            }
        }
    }

    public List<InvoiceResponse> getInvoicesByStudentId(String studentId) {

        // Ensure account exists before fetching invoices
        financeAccountRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Finance account not found for student"));

        // Return invoices sorted by latest first (used for UI display)
        return invoiceRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::mapToInvoiceResponse)
                .toList();
    }

    //Maps invoice entity to response dto
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