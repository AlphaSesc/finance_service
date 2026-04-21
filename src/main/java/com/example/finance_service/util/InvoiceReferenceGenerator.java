package com.example.finance_service.util;

import java.util.UUID;

// Utility class for generating unique invoice references
public class InvoiceReferenceGenerator {

    // Prevent instantiation of utility class
    private InvoiceReferenceGenerator() {}

    // Generates a short unique reference identifier for invoices
    public static String generate() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}