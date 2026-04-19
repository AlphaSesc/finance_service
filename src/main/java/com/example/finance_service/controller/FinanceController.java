package com.example.finance_service.controller;

import com.example.finance_service.dto.*;
import com.example.finance_service.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/accounts")
    public FinanceAccountResponse createAccount(@RequestBody CreateFinanceAccountRequest request) {
        return financeService.createAccount(request);
    }

    @PostMapping("/invoices")
    public InvoiceResponse createInvoice(@RequestBody CreateInvoiceRequest request) {
        return financeService.createInvoice(request);
    }

    @GetMapping("/invoices/outstanding/{studentId}")
    public OutstandingBalanceResponse checkOutstandingBalance(@PathVariable String studentId) {
        return financeService.checkOutstandingBalance(studentId);
    }

    @PutMapping("/invoices/pay")
    public InvoiceResponse payInvoice(@RequestBody PayInvoiceRequest request) {
        return financeService.payInvoice(request);
    }

    @GetMapping("/student/{studentId}")
    public List<InvoiceResponse> getInvoicesByStudentId(@PathVariable String studentId) {
        return financeService.getInvoicesByStudentId(studentId);
    }
}