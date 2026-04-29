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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FinanceAccountRepository financeAccountRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private FinanceService financeService;

    private FinanceAccount financeAccount;
    private Invoice invoice;

    @BeforeEach
    void setUp() {

        financeAccount = FinanceAccount.builder()
                .id(1L)
                .studentId("STU-100")
                .email("student@example.com")
                .build();

        invoice = Invoice.builder()
                .id(1L)
                .studentId("STU-100")
                .courseCode("CS101")
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .amount(BigDecimal.valueOf(2500))
                .reference("INV-001")
                .status(InvoiceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createAccountShouldCreateFinanceAccount() {

        CreateFinanceAccountRequest request = CreateFinanceAccountRequest.builder()
                .studentId("STU-100")
                .email("student@example.com")
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.empty());

        when(financeAccountRepository.save(any(FinanceAccount.class)))
                .thenReturn(financeAccount);

        FinanceAccountResponse response = financeService.createAccount(request);

        assertNotNull(response);
        assertEquals("STU-100", response.getStudentId());
        assertEquals("student@example.com", response.getEmail());

        verify(financeAccountRepository).save(any(FinanceAccount.class));
    }

    @Test
    void createAccountShouldThrowWhenAccountAlreadyExists() {

        CreateFinanceAccountRequest request = CreateFinanceAccountRequest.builder()
                .studentId("STU-100")
                .email("student@example.com")
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> financeService.createAccount(request)
        );

        assertEquals("Finance account already exists for student", exception.getMessage());

        verify(financeAccountRepository, never()).save(any());
    }

    @Test
    void createInvoiceShouldCreateInvoiceSuccessfully() {

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .studentId("STU-100")
                .courseCode("CS101")
                .amount(BigDecimal.valueOf(2500))
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        when(invoiceRepository.save(any(Invoice.class)))
                .thenReturn(invoice);

        InvoiceResponse response = financeService.createInvoice(request);

        assertNotNull(response);
        assertEquals("STU-100", response.getStudentId());
        assertEquals("CS101", response.getCourseCode());
        assertEquals(InvoiceStatus.PENDING, response.getStatus());

        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoiceShouldThrowWhenFinanceAccountDoesNotExist() {

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .studentId("STU-404")
                .courseCode("CS101")
                .amount(BigDecimal.valueOf(2500))
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .build();

        when(financeAccountRepository.findByStudentId("STU-404"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> financeService.createInvoice(request)
        );

        assertEquals("Finance account not found for student", exception.getMessage());

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createInvoiceShouldThrowWhenInvoiceTypeIsMissing() {

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .studentId("STU-100")
                .amount(BigDecimal.valueOf(2500))
                .invoiceType(null)
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> financeService.createInvoice(request)
        );

        assertEquals("Invoice type is required", exception.getMessage());
    }

    @Test
    void createInvoiceShouldThrowWhenCourseCodeMissingForCourseEnrollment() {

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .studentId("STU-100")
                .amount(BigDecimal.valueOf(2500))
                .invoiceType(InvoiceType.COURSE_ENROLLMENT)
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> financeService.createInvoice(request)
        );

        assertEquals(
                "Course code is required for course enrollment invoice",
                exception.getMessage()
        );
    }

    @Test
    void createInvoiceShouldThrowWhenLibraryFineContainsCourseCode() {

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .studentId("STU-100")
                .courseCode("CS101")
                .amount(BigDecimal.valueOf(100))
                .invoiceType(InvoiceType.LIBRARY_FINE)
                .build();

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> financeService.createInvoice(request)
        );

        assertEquals(
                "Course code must not be provided for library fine invoice",
                exception.getMessage()
        );
    }

    @Test
    void checkOutstandingBalanceShouldReturnTrueWhenPendingInvoicesExist() {

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        when(invoiceRepository.existsByStudentIdAndStatus(
                "STU-100",
                InvoiceStatus.PENDING
        )).thenReturn(true);

        OutstandingBalanceResponse response =
                financeService.checkOutstandingBalance("STU-100");

        assertTrue(response.isHasOutstandingBalance());
        assertEquals("STU-100", response.getStudentId());
    }

    @Test
    void checkOutstandingBalanceShouldThrowWhenFinanceAccountDoesNotExist() {

        when(financeAccountRepository.findByStudentId("STU-404"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> financeService.checkOutstandingBalance("STU-404")
        );

        assertEquals(
                "Finance account not found for student",
                exception.getMessage()
        );
    }

    @Test
    void payInvoiceShouldMarkInvoiceAsPaid() {

        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .studentId("STU-100")
                .reference("INV-001")
                .build();

        when(invoiceRepository.findByStudentIdAndReference(
                "STU-100",
                "INV-001"
        )).thenReturn(Optional.of(invoice));

        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> {
                    Invoice savedInvoice = invocation.getArgument(0);
                    return savedInvoice;
                });

        InvoiceResponse response = financeService.payInvoice(request);

        assertEquals(InvoiceStatus.PAID, response.getStatus());

        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void payInvoiceShouldThrowWhenInvoiceNotFound() {

        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .studentId("STU-100")
                .reference("INV-404")
                .build();

        when(invoiceRepository.findByStudentIdAndReference(
                "STU-100",
                "INV-404"
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> financeService.payInvoice(request)
        );

        assertEquals("Invoice not found", exception.getMessage());
    }

    @Test
    void payInvoiceShouldThrowWhenInvoiceAlreadyPaid() {

        invoice.setStatus(InvoiceStatus.PAID);

        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .studentId("STU-100")
                .reference("INV-001")
                .build();

        when(invoiceRepository.findByStudentIdAndReference(
                "STU-100",
                "INV-001"
        )).thenReturn(Optional.of(invoice));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> financeService.payInvoice(request)
        );

        assertEquals("Invoice is already paid", exception.getMessage());

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getInvoicesByStudentIdShouldReturnInvoices() {

        when(financeAccountRepository.findByStudentId("STU-100"))
                .thenReturn(Optional.of(financeAccount));

        when(invoiceRepository.findByStudentIdOrderByCreatedAtDesc("STU-100"))
                .thenReturn(List.of(invoice));

        List<InvoiceResponse> responses =
                financeService.getInvoicesByStudentId("STU-100");

        assertEquals(1, responses.size());
        assertEquals("INV-001", responses.get(0).getReference());
    }

    @Test
    void getInvoicesByStudentIdShouldThrowWhenFinanceAccountDoesNotExist() {

        when(financeAccountRepository.findByStudentId("STU-404"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> financeService.getInvoicesByStudentId("STU-404")
        );

        assertEquals(
                "Finance account not found for student",
                exception.getMessage()
        );
    }
}