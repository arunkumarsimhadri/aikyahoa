package com.aikya.domain.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AssociationConfigLookupPort;
import com.aikya.common.AssociationConfigLookupPort.AssociationBillingConfig;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.BillingType;
import com.aikya.common.NotificationPort;
import com.aikya.common.SecurityContextHelper;
import com.aikya.common.TenantContext;
import com.aikya.domain.dto.BillingRunRequest;
import com.aikya.domain.dto.InvoiceDto;
import com.aikya.domain.dto.MaintenanceExpenseCreateRequest;
import com.aikya.domain.dto.MaintenanceExpenseDto;
import com.aikya.domain.entity.Invoice;
import com.aikya.domain.entity.MaintenanceExpense;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.repository.InvoiceRepository;
import com.aikya.domain.repository.MaintenanceExpenseRepository;
import com.aikya.domain.repository.UnitRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BillingService {

    private final UnitRepository unitRepository;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceExpenseRepository maintenanceExpenseRepository;
    private final ObjectProvider<AssociationConfigLookupPort> configLookupPortProvider;
    private final ObjectProvider<NotificationPort> notificationPortProvider;

    public BillingService(
            UnitRepository unitRepository,
            InvoiceRepository invoiceRepository,
            MaintenanceExpenseRepository maintenanceExpenseRepository,
            ObjectProvider<AssociationConfigLookupPort> configLookupPortProvider,
            ObjectProvider<NotificationPort> notificationPortProvider
    ) {
        this.unitRepository = unitRepository;
        this.invoiceRepository = invoiceRepository;
        this.maintenanceExpenseRepository = maintenanceExpenseRepository;
        this.configLookupPortProvider = configLookupPortProvider;
        this.notificationPortProvider = notificationPortProvider;
    }

    @Transactional(readOnly = true)
    public List<Unit> listUnits() {
        return unitRepository.findAllByOrderByBlockNameAscUnitNumberAsc();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> listInvoices(Long unitId, String status) {
        List<Invoice> invoices;
        if (unitId != null) {
            invoices = invoiceRepository.findByUnitIdOrderByBillingYearDescBillingMonthDesc(unitId);
        } else if (status != null && !status.isBlank()) {
            invoices = invoiceRepository.findByStatusOrderByDueDateAsc(status.trim().toUpperCase());
        } else {
            invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        }

        Map<Long, Unit> unitMap = unitRepository.findAll().stream()
                .collect(Collectors.toMap(Unit::getId, Function.identity()));

        return invoices.stream()
                .map(inv -> toInvoiceDto(inv, unitMap.get(inv.getUnitId())))
                .toList();
    }

    @Transactional
    public List<InvoiceDto> executeBillingRun(BillingRunRequest request) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().canReconcilePayments()) {
            throw new AikyaException(HttpStatus.FORBIDDEN,
                    "Only Treasurer, President, or Super Admin can execute monthly billing runs");
        }

        LocalDate now = LocalDate.now();
        int month = (request != null && request.billingMonth() != null) ? request.billingMonth() : now.getMonthValue();
        int year = (request != null && request.billingYear() != null) ? request.billingYear() : now.getYear();
        LocalDate dueDate = (request != null && request.dueDate() != null)
                ? request.dueDate()
                : LocalDate.of(year, month, 10).plusMonths(month == now.getMonthValue() && now.getDayOfMonth() > 10 ? 1 : 0);
        BigDecimal otherCharges = (request != null && request.otherCharges() != null)
                ? request.otherCharges().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String notes = (request != null && request.notes() != null && !request.notes().isBlank())
                ? request.notes()
                : "Monthly Maintenance Invoice for " + String.format("%02d/%d", month, year);

        AssociationBillingConfig config = resolveBillingConfig();
        List<Unit> units = unitRepository.findAllByOrderByBlockNameAscUnitNumberAsc();
        List<InvoiceDto> generated = new ArrayList<>();

        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();

        for (Unit unit : units) {
            if (invoiceRepository.existsByUnitIdAndBillingMonthAndBillingYear(unit.getId(), month, year)) {
                continue;
            }

            BigDecimal maintenanceCharge = calculateMaintenanceCharge(unit, config);
            BigDecimal sinkingFund = config.sinkingFundAmount() != null
                    ? config.sinkingFundAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal waterCharge = config.waterChargePerUnit() != null
                    ? config.waterChargePerUnit().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal lateFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = maintenanceCharge
                    .add(sinkingFund)
                    .add(waterCharge)
                    .add(otherCharges)
                    .add(lateFee)
                    .setScale(2, RoundingMode.HALF_UP);

            Invoice invoice = new Invoice();
            String assocCode = config.code() != null ? config.code() : "ELITE";
            invoice.setInvoiceNumber(String.format("INV-%s-%d%02d-%s", assocCode, year, month, unit.getUnitNumber()));
            invoice.setUnitId(unit.getId());
            invoice.setBillingMonth(month);
            invoice.setBillingYear(year);
            invoice.setDueDate(dueDate);
            invoice.setMaintenanceCharge(maintenanceCharge);
            invoice.setSinkingFundCharge(sinkingFund);
            invoice.setWaterCharge(waterCharge);
            invoice.setOtherCharges(otherCharges);
            invoice.setLateFee(lateFee);
            invoice.setTotalAmount(totalAmount);
            invoice.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            invoice.setStatus("PENDING");
            invoice.setNotes(notes);
            invoice.setCreatedAt(Instant.now());

            Invoice saved = invoiceRepository.save(invoice);
            generated.add(toInvoiceDto(saved, unit));

            if (notificationPort != null && unit.getOwnerPhone() != null && !unit.getOwnerPhone().isBlank()) {
                notificationPort.sendNotification(
                        unit.getOwnerUserId(),
                        unit.getOwnerPhone(),
                        unit.getOwnerName() != null ? unit.getOwnerName() : "Resident",
                        currentUser.preferredLocale() != null ? currentUser.preferredLocale() : "en",
                        "SMS",
                        "INVOICE_GENERATED",
                        Map.of(
                                "unitNumber", unit.getUnitNumber(),
                                "invoiceNumber", saved.getInvoiceNumber(),
                                "amount", saved.getTotalAmount().toPlainString(),
                                "dueDate", saved.getDueDate().toString(),
                                "associationName", config.name()
                        )
                );
            }
        }

        return generated;
    }

    public BigDecimal calculateMaintenanceCharge(Unit unit, AssociationBillingConfig config) {
        BillingType type = config.billingType() != null ? config.billingType() : BillingType.AREA_BASED;
        BigDecimal area = BigDecimal.valueOf(unit.getAreaSqft() != null ? unit.getAreaSqft() : 1500);
        BigDecimal ratePerSqft = config.ratePerSqft() != null ? config.ratePerSqft() : new BigDecimal("3.50");
        BigDecimal flatAmount = config.flatMaintenanceAmount() != null ? config.flatMaintenanceAmount() : new BigDecimal("3500.00");

        return switch (type) {
            case AREA_BASED -> area.multiply(ratePerSqft).setScale(2, RoundingMode.HALF_UP);
            case FLAT_RATE -> flatAmount.setScale(2, RoundingMode.HALF_UP);
            case HYBRID -> flatAmount.add(
                    area.multiply(ratePerSqft).multiply(new BigDecimal("0.5"))
            ).setScale(2, RoundingMode.HALF_UP);
        };
    }

    @Transactional(readOnly = true)
    public List<MaintenanceExpenseDto> listExpenses(String category) {
        List<MaintenanceExpense> expenses;
        if (category != null && !category.isBlank()) {
            expenses = maintenanceExpenseRepository.findByCategoryOrderByExpenseDateDescCreatedAtDesc(category.trim().toUpperCase());
        } else {
            expenses = maintenanceExpenseRepository.findAllByOrderByExpenseDateDescCreatedAtDesc();
        }
        return expenses.stream().map(this::toExpenseDto).toList();
    }

    @Transactional
    public MaintenanceExpenseDto createExpense(MaintenanceExpenseCreateRequest request) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().isManagingCommittee()) {
            throw new AikyaException(HttpStatus.FORBIDDEN,
                    "Only Managing Committee members can record apartment maintenance expenses");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Expense title is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Expense amount must be greater than zero");
        }

        MaintenanceExpense expense = new MaintenanceExpense();
        expense.setExpenseNumber("EXP-" + LocalDate.now().getYear() + "-" + (System.currentTimeMillis() % 1000000));
        expense.setCategory(request.category() != null && !request.category().isBlank()
                ? request.category().trim().toUpperCase() : "MAINTENANCE");
        expense.setTitle(request.title().trim());
        expense.setTitleTe(request.titleTe());
        expense.setVendorName(request.vendorName() != null && !request.vendorName().isBlank()
                ? request.vendorName().trim() : "General Vendor");
        expense.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setExpenseDate(request.expenseDate() != null ? request.expenseDate() : LocalDate.now());
        expense.setPaymentMode(request.paymentMode() != null && !request.paymentMode().isBlank()
                ? request.paymentMode().trim().toUpperCase() : "NEFT");
        expense.setTransactionReference(request.transactionReference());
        List<String> rawUrls = new ArrayList<>();
        if (request.receiptFileUrls() != null) {
            rawUrls.addAll(request.receiptFileUrls());
        }
        if (request.attachments() != null) {
            rawUrls.addAll(request.attachments());
        }
        if (!rawUrls.isEmpty()) {
            List<String> cleaned = rawUrls.stream()
                    .filter(u -> u != null && !u.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
            expense.setReceiptFileUrls(String.join(",", cleaned));
        }
        expense.setRecordedByUserId(currentUser.userId());
        expense.setRecordedByName(currentUser.fullName());
        expense.setApprovedByName(request.approvedByName() != null && !request.approvedByName().isBlank()
                ? request.approvedByName() : currentUser.fullName());
        expense.setStatus("APPROVED");
        expense.setNotes(request.notes());
        expense.setCreatedAt(Instant.now());

        MaintenanceExpense saved = maintenanceExpenseRepository.save(expense);
        return toExpenseDto(saved);
    }

    private AssociationBillingConfig resolveBillingConfig() {
        String currentSchema = TenantContext.getCurrentTenant();
        AssociationConfigLookupPort port = configLookupPortProvider.getIfAvailable();
        if (port != null) {
            AssociationBillingConfig config = port.getCurrentAssociationConfig(currentSchema);
            if (config != null) {
                return config;
            }
        }
        return new AssociationBillingConfig(
                1L,
                "Elite Towers",
                "ELITE_TOWERS_ONGOLE",
                currentSchema,
                BillingType.AREA_BASED,
                new BigDecimal("3.50"),
                new BigDecimal("3500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("450.00"),
                new BigDecimal("2.00")
        );
    }

    public InvoiceDto toInvoiceDto(Invoice inv, Unit unit) {
        BigDecimal total = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balance = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new InvoiceDto(
                inv.getId(),
                inv.getInvoiceNumber(),
                inv.getUnitId(),
                unit != null ? unit.getUnitNumber() : "N/A",
                unit != null ? unit.getBlockName() : "Block A",
                unit != null ? unit.getOwnerName() : "Resident",
                inv.getBillingMonth(),
                inv.getBillingYear(),
                inv.getDueDate(),
                inv.getMaintenanceCharge(),
                inv.getSinkingFundCharge(),
                inv.getWaterCharge(),
                inv.getOtherCharges(),
                inv.getLateFee(),
                total,
                paid,
                balance,
                inv.getStatus(),
                inv.getNotes(),
                inv.getCreatedAt()
        );
    }

    public MaintenanceExpenseDto toExpenseDto(MaintenanceExpense exp) {
        List<String> urls = List.of();
        if (exp.getReceiptFileUrls() != null && !exp.getReceiptFileUrls().isBlank()) {
            urls = Arrays.stream(exp.getReceiptFileUrls().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return new MaintenanceExpenseDto(
                exp.getId(),
                exp.getExpenseNumber(),
                exp.getCategory(),
                exp.getTitle(),
                exp.getTitleTe(),
                exp.getVendorName(),
                exp.getAmount(),
                exp.getExpenseDate(),
                exp.getPaymentMode(),
                exp.getTransactionReference(),
                urls,
                urls,
                exp.getRecordedByUserId(),
                exp.getRecordedByName(),
                exp.getApprovedByName(),
                exp.getStatus(),
                exp.getNotes(),
                exp.getCreatedAt()
        );
    }
}
