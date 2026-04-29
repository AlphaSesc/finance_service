package com.example.finance_service.integration;

import com.example.finance_service.dto.FinanceAccountResponse;
import com.example.finance_service.dto.InvoiceResponse;
import com.example.finance_service.dto.OutstandingBalanceResponse;
import com.example.finance_service.entity.FinanceAccount;
import com.example.finance_service.entity.Invoice;
import com.example.finance_service.entity.InvoiceStatus;
import com.example.finance_service.entity.InvoiceType;
import com.example.finance_service.repository.FinanceAccountRepository;
import com.example.finance_service.repository.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for FinanceController covering account creation, invoice management,
// payment processing, and balance/history queries.
// No authentication is required for finance endpoints.
class FinanceControllerIntegrationTest {

    // Testcontainers: real MySQL container as backend database
    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    // Random port assigned to embedded server
    @LocalServerPort
    private int port;

    // Spring-managed dependencies
    @Autowired
    private FinanceAccountRepository financeAccountRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    // Shared test state
    private RestClient restClient;

    // Setup & Teardown
    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server.
        // Custom status handler prevents RestClient from throwing on 4xx/5xx,
        // so we can assert error statuses directly.
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();
    }

    @AfterEach
    void tearDown() {
        // Clear in dependency-safe order: invoices → accounts
        invoiceRepository.deleteAll();
        financeAccountRepository.deleteAll();
    }

    // Helpers
    private FinanceAccount saveAccount(String studentId, String email) {
        FinanceAccount account = FinanceAccount.builder()
                .studentId(studentId)
                .email(email)
                .build();
        return financeAccountRepository.save(account);
    }

    private Invoice savePendingInvoice(String studentId, String reference, BigDecimal amount,
                                       InvoiceType type, String courseCode) {
        Invoice invoice = Invoice.builder()
                .studentId(studentId)
                .courseCode(courseCode)
                .amount(amount)
                .reference(reference)
                .status(InvoiceStatus.PENDING)
                .invoiceType(type)
                .createdAt(LocalDateTime.now())
                .build();
        return invoiceRepository.save(invoice);
    }


    // Test 1 – POST /api/accounts  →  successfully creates a finance account
    @Test
    void shouldCreateFinanceAccountSuccessfully() {
        // Given – payload with studentId and email
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("email", "student1@test.com");

        // When – POST /api/accounts is called
        ResponseEntity<FinanceAccountResponse> response = restClient.post()
                .uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(FinanceAccountResponse.class);

        // Then – response is 200 OK with account details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-001");
        assertThat(response.getBody().getEmail()).isEqualTo("student1@test.com");

        // Verify account is persisted
        assertThat(financeAccountRepository.findByStudentId("STU-001")).isPresent();
    }


    // Test 2 – POST /api/accounts  →  fails when account already exists
    @Test
    void shouldFailCreatingAccountWhenStudentAlreadyHasOne() {
        // Given – an account already exists for this student
        saveAccount("STU-001", "existing@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("email", "newemail@test.com");

        // When – POST /api/accounts is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify only one account exists
        assertThat(financeAccountRepository.findAll()).hasSize(1);
    }

    // Test 3 – POST /api/invoices  →  successfully creates COURSE_ENROLLMENT invoice
    @Test
    void shouldCreateCourseEnrollmentInvoiceSuccessfully() {
        // Given – a finance account exists for the student
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("courseCode", "CS101");
        request.put("amount", 1500.00);
        request.put("invoiceType", "COURSE_ENROLLMENT");

        // When – POST /api/invoices is called
        ResponseEntity<InvoiceResponse> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(InvoiceResponse.class);

        // Then – response is 200 OK with invoice details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-001");
        assertThat(response.getBody().getCourseCode()).isEqualTo("CS101");
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("1500.00");
        assertThat(response.getBody().getInvoiceType()).isEqualTo(InvoiceType.COURSE_ENROLLMENT);
        assertThat(response.getBody().getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(response.getBody().getReference()).isNotNull().startsWith("INV-");
        assertThat(response.getBody().getCreatedAt()).isNotNull();

        // Verify invoice is persisted
        assertThat(invoiceRepository.findAll()).hasSize(1);
    }

    
    // Test 4 – POST /api/invoices  →  successfully creates LIBRARY_FINE invoice
    @Test
    void shouldCreateLibraryFineInvoiceSuccessfully() {
        // Given – a finance account exists for the student
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        // courseCode is null for LIBRARY_FINE
        request.put("amount", 500.00);
        request.put("invoiceType", "LIBRARY_FINE");

        // When – POST /api/invoices is called
        ResponseEntity<InvoiceResponse> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(InvoiceResponse.class);

        // Then – response is 200 OK with invoice details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCourseCode()).isNull();
        assertThat(response.getBody().getInvoiceType()).isEqualTo(InvoiceType.LIBRARY_FINE);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("500.00");
    }

    
    // Test 5 – POST /api/invoices  →  fails when account doesn't exist
    @Test
    void shouldFailCreatingInvoiceWhenAccountNotFound() {
        // Given – no account exists

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-DOES-NOT-EXIST");
        request.put("courseCode", "CS101");
        request.put("amount", 1500.00);
        request.put("invoiceType", "COURSE_ENROLLMENT");

        // When – POST /api/invoices is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify no invoice was created
        assertThat(invoiceRepository.findAll()).isEmpty();
    }

    
    // Test 6 – POST /api/invoices  →  fails for COURSE_ENROLLMENT without courseCode
    @Test
    void shouldFailCreatingCourseInvoiceWithoutCourseCode() {
        // Given – an account exists
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        // courseCode missing for COURSE_ENROLLMENT
        request.put("amount", 1500.00);
        request.put("invoiceType", "COURSE_ENROLLMENT");

        // When – POST /api/invoices is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidRequestException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 7 – POST /api/invoices  →  fails for LIBRARY_FINE WITH courseCode
    @Test
    void shouldFailCreatingLibraryFineWithCourseCode() {
        // Given – an account exists
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("courseCode", "CS101");  // should NOT be present for library fine
        request.put("amount", 500.00);
        request.put("invoiceType", "LIBRARY_FINE");

        // When – POST /api/invoices is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidRequestException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 8 – POST /api/invoices  →  fails when invoice type is missing
    @Test
    void shouldFailCreatingInvoiceWithoutType() {
        // Given – an account exists
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("courseCode", "CS101");
        request.put("amount", 1500.00);
        // invoiceType missing

        // When – POST /api/invoices is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidRequestException: "Invoice type is required")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 9 – GET /api/invoices/outstanding/{studentId}  →  has outstanding
    @Test
    void shouldReturnTrueWhenStudentHasOutstandingBalance() {
        // Given – account with at least one PENDING invoice
        saveAccount("STU-001", "student@test.com");
        savePendingInvoice("STU-001", "INV-AAA", new BigDecimal("1500.00"),
                InvoiceType.COURSE_ENROLLMENT, "CS101");

        // When – GET /api/invoices/outstanding/{id}
        ResponseEntity<OutstandingBalanceResponse> response = restClient.get()
                .uri("/api/invoices/outstanding/STU-001")
                .retrieve()
                .toEntity(OutstandingBalanceResponse.class);

        // Then – hasOutstandingBalance is true
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-001");
        assertThat(response.getBody().isHasOutstandingBalance()).isTrue();
    }

    
    // Test 10 – GET /api/invoices/outstanding/{studentId}  →  no outstanding
    @Test
    void shouldReturnFalseWhenAllInvoicesPaid() {
        // Given – account with one PAID invoice
        saveAccount("STU-001", "student@test.com");
        Invoice invoice = savePendingInvoice("STU-001", "INV-AAA", new BigDecimal("1500.00"),
                InvoiceType.COURSE_ENROLLMENT, "CS101");
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        // When – GET /api/invoices/outstanding/{id}
        ResponseEntity<OutstandingBalanceResponse> response = restClient.get()
                .uri("/api/invoices/outstanding/STU-001")
                .retrieve()
                .toEntity(OutstandingBalanceResponse.class);

        // Then – hasOutstandingBalance is false
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isHasOutstandingBalance()).isFalse();
    }

    
    // Test 11 – GET /api/invoices/outstanding/{studentId}  →  no invoices
    @Test
    void shouldReturnFalseWhenStudentHasNoInvoices() {
        // Given – account exists but no invoices
        saveAccount("STU-001", "student@test.com");

        // When – GET /api/invoices/outstanding/{id}
        ResponseEntity<OutstandingBalanceResponse> response = restClient.get()
                .uri("/api/invoices/outstanding/STU-001")
                .retrieve()
                .toEntity(OutstandingBalanceResponse.class);

        // Then – hasOutstandingBalance is false
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().isHasOutstandingBalance()).isFalse();
    }

    
    // Test 12 – GET /api/invoices/outstanding/{studentId}  →  fails when account missing
    @Test
    void shouldFailOutstandingCheckWhenAccountNotFound() {
        // Given – no account exists

        // When – GET /api/invoices/outstanding/{id}
        ResponseEntity<String> response = restClient.get()
                .uri("/api/invoices/outstanding/STU-DOES-NOT-EXIST")
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 13 – PUT /api/invoices/pay  →  successfully pays an invoice
    @Test
    void shouldPayInvoiceSuccessfully() {
        // Given – a PENDING invoice
        saveAccount("STU-001", "student@test.com");
        Invoice invoice = savePendingInvoice("STU-001", "INV-AAA",
                new BigDecimal("1500.00"), InvoiceType.COURSE_ENROLLMENT, "CS101");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("reference", "INV-AAA");

        // When – PUT /api/invoices/pay
        ResponseEntity<InvoiceResponse> response = restClient.put()
                .uri("/api/invoices/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(InvoiceResponse.class);

        // Then – response is 200 OK and status is PAID
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getBody().getReference()).isEqualTo("INV-AAA");

        // Verify status persisted in DB
        Invoice updated = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    
    // Test 14 – PUT /api/invoices/pay  →  fails on duplicate payment
    @Test
    void shouldFailPayingAlreadyPaidInvoice() {
        // Given – an already PAID invoice
        saveAccount("STU-001", "student@test.com");
        Invoice invoice = savePendingInvoice("STU-001", "INV-AAA",
                new BigDecimal("1500.00"), InvoiceType.COURSE_ENROLLMENT, "CS101");
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("reference", "INV-AAA");

        // When – PUT /api/invoices/pay called again
        ResponseEntity<String> response = restClient.put()
                .uri("/api/invoices/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException: "Invoice is already paid")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 15 – PUT /api/invoices/pay  →  fails when invoice not found
    @Test
    void shouldFailPayingInvoiceWithInvalidReference() {
        // Given – an account but no invoice with this reference
        saveAccount("STU-001", "student@test.com");

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-001");
        request.put("reference", "INV-DOES-NOT-EXIST");

        // When – PUT /api/invoices/pay
        ResponseEntity<String> response = restClient.put()
                .uri("/api/invoices/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Invoice not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 16 – PUT /api/invoices/pay  →  fails when studentId/reference don't match
    //          (security: invoice belongs to different student)
    @Test
    void shouldFailPayingInvoiceWhenStudentIdDoesNotMatchReference() {
        // Given – two students, invoice belongs to STU-001
        saveAccount("STU-001", "student1@test.com");
        saveAccount("STU-002", "student2@test.com");
        savePendingInvoice("STU-001", "INV-AAA",
                new BigDecimal("1500.00"), InvoiceType.COURSE_ENROLLMENT, "CS101");

        // STU-002 tries to pay STU-001's invoice
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-002");
        request.put("reference", "INV-AAA");

        // When – PUT /api/invoices/pay
        ResponseEntity<String> response = restClient.put()
                .uri("/api/invoices/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (composite key lookup fails → ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 17 – GET /api/invoices/student/{studentId}  →  returns history
    @Test
    void shouldGetInvoiceHistoryByStudentId() {
        // Given – student with multiple invoices
        saveAccount("STU-001", "student@test.com");
        savePendingInvoice("STU-001", "INV-AAA", new BigDecimal("1500.00"),
                InvoiceType.COURSE_ENROLLMENT, "CS101");
        savePendingInvoice("STU-001", "INV-BBB", new BigDecimal("500.00"),
                InvoiceType.LIBRARY_FINE, null);

        // When – GET /api/invoices/student/{id}
        ResponseEntity<InvoiceResponse[]> response = restClient.get()
                .uri("/api/invoices/student/STU-001")
                .retrieve()
                .toEntity(InvoiceResponse[].class);

        // Then – response contains both invoices
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(InvoiceResponse::getReference)
                .containsExactlyInAnyOrder("INV-AAA", "INV-BBB");
    }

    
    // Test 18 – GET /api/invoices/student/{studentId}  →  empty when no invoices
    @Test
    void shouldReturnEmptyHistoryWhenStudentHasNoInvoices() {
        // Given – account with no invoices
        saveAccount("STU-001", "student@test.com");

        // When – GET /api/invoices/student/{id}
        ResponseEntity<InvoiceResponse[]> response = restClient.get()
                .uri("/api/invoices/student/STU-001")
                .retrieve()
                .toEntity(InvoiceResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    
    // Test 19 – GET /api/invoices/student/{studentId}  →  fails when account missing
    @Test
    void shouldFailGettingInvoicesWhenAccountNotFound() {
        // Given – no account exists

        // When – GET /api/invoices/student/{id}
        ResponseEntity<String> response = restClient.get()
                .uri("/api/invoices/student/STU-DOES-NOT-EXIST")
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    
    // Test 20 – GET /api/invoices/student/{studentId}  →  isolation between students
    @Test
    void shouldNotReturnOtherStudentsInvoices() {
        // Given – two students with their own invoices
        saveAccount("STU-001", "student1@test.com");
        saveAccount("STU-002", "student2@test.com");
        savePendingInvoice("STU-001", "INV-AAA", new BigDecimal("1500.00"),
                InvoiceType.COURSE_ENROLLMENT, "CS101");
        savePendingInvoice("STU-002", "INV-BBB", new BigDecimal("500.00"),
                InvoiceType.LIBRARY_FINE, null);

        // When – GET invoices for STU-001
        ResponseEntity<InvoiceResponse[]> response = restClient.get()
                .uri("/api/invoices/student/STU-001")
                .retrieve()
                .toEntity(InvoiceResponse[].class);

        // Then – only STU-001's invoice is returned
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(1);
        assertThat(response.getBody()[0].getReference()).isEqualTo("INV-AAA");
        assertThat(response.getBody()[0].getStudentId()).isEqualTo("STU-001");
    }

    
    // Test 21 – Full flow: create account → create invoice → pay → check balance
    @Test
    void shouldCompleteFullFinanceFlow() {
        // ---- Step 1: Create account ----
        Map<String, Object> accountRequest = new HashMap<>();
        accountRequest.put("studentId", "STU-FLOW-001");
        accountRequest.put("email", "flow@test.com");

        ResponseEntity<FinanceAccountResponse> accountResponse = restClient.post()
                .uri("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(accountRequest)
                .retrieve()
                .toEntity(FinanceAccountResponse.class);

        assertThat(accountResponse.getStatusCode().value()).isEqualTo(200);

        // ---- Step 2: Create invoice ----
        Map<String, Object> invoiceRequest = new HashMap<>();
        invoiceRequest.put("studentId", "STU-FLOW-001");
        invoiceRequest.put("courseCode", "CS101");
        invoiceRequest.put("amount", 1500.00);
        invoiceRequest.put("invoiceType", "COURSE_ENROLLMENT");

        ResponseEntity<InvoiceResponse> invoiceResponse = restClient.post()
                .uri("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invoiceRequest)
                .retrieve()
                .toEntity(InvoiceResponse.class);

        assertThat(invoiceResponse.getStatusCode().value()).isEqualTo(200);
        String reference = invoiceResponse.getBody().getReference();

        // ---- Step 3: Check outstanding balance (should be true) ----
        ResponseEntity<OutstandingBalanceResponse> balanceBefore = restClient.get()
                .uri("/api/invoices/outstanding/STU-FLOW-001")
                .retrieve()
                .toEntity(OutstandingBalanceResponse.class);

        assertThat(balanceBefore.getBody().isHasOutstandingBalance()).isTrue();

        // ---- Step 4: Pay the invoice ----
        Map<String, Object> payRequest = new HashMap<>();
        payRequest.put("studentId", "STU-FLOW-001");
        payRequest.put("reference", reference);

        ResponseEntity<InvoiceResponse> payResponse = restClient.put()
                .uri("/api/invoices/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payRequest)
                .retrieve()
                .toEntity(InvoiceResponse.class);

        assertThat(payResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(payResponse.getBody().getStatus()).isEqualTo(InvoiceStatus.PAID);

        // ---- Step 5: Check outstanding balance again (should be false) ----
        ResponseEntity<OutstandingBalanceResponse> balanceAfter = restClient.get()
                .uri("/api/invoices/outstanding/STU-FLOW-001")
                .retrieve()
                .toEntity(OutstandingBalanceResponse.class);

        assertThat(balanceAfter.getBody().isHasOutstandingBalance()).isFalse();
    }
}