package com.example.finance_service.controller;

import com.example.finance_service.dto.*;
import com.example.finance_service.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
// REST controller exposing finance-related endpoints for account and invoice operations
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/accounts")
    // Creates a finance account for a student (triggered during enrollment)
    public FinanceAccountResponse createAccount(@RequestBody CreateFinanceAccountRequest request) {
        return financeService.createAccount(request);
    }

    @PostMapping("/invoices")
    // Generates a new invoice (e.g., course enrollment or library fine)
    public InvoiceResponse createInvoice(@RequestBody CreateInvoiceRequest request) {
        return financeService.createInvoice(request);
    }

    @GetMapping("/invoices/outstanding/{studentId}")
    // Checks if a student has any unpaid invoices (used for eligibility checks)
    public OutstandingBalanceResponse checkOutstandingBalance(@PathVariable String studentId) {
        return financeService.checkOutstandingBalance(studentId);
    }

    @PutMapping("/invoices/pay")
    // Processes invoice payment using studentId and reference
    public InvoiceResponse payInvoice(@RequestBody PayInvoiceRequest request) {
        return financeService.payInvoice(request);
    }

    @GetMapping("/invoices/student/{studentId}")
    // Retrieves all invoices of a student for history display
    public List<InvoiceResponse> getInvoicesByStudentId(@PathVariable String studentId) {
        return financeService.getInvoicesByStudentId(studentId);
    }
}