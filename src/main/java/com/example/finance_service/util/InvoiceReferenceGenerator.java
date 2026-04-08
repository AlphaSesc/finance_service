package com.example.finance_service.util;

import java.util.UUID;

public class InvoiceReferenceGenerator {

    private InvoiceReferenceGenerator() {}

    public static String generate() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}