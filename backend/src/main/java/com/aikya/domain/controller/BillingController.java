package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.BillingRunRequest;
import com.aikya.domain.dto.InvoiceDto;
import com.aikya.domain.dto.MaintenanceExpenseCreateRequest;
import com.aikya.domain.dto.MaintenanceExpenseDto;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.service.BillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/units")
    public ApiResponse<List<Unit>> listUnits() {
        return ApiResponse.ok(billingService.listUnits());
    }

    @GetMapping("/billing/invoices")
    public ApiResponse<List<InvoiceDto>> listInvoices(
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(billingService.listInvoices(unitId, status));
    }

    @PostMapping("/billing/runs")
    public ApiResponse<List<InvoiceDto>> executeBillingRun(@RequestBody(required = false) BillingRunRequest request) {
        List<InvoiceDto> generated = billingService.executeBillingRun(request);
        return ApiResponse.ok("Generated " + generated.size() + " monthly maintenance invoices", generated);
    }

    @GetMapping({"/billing/expenses", "/expenses"})
    public ApiResponse<List<MaintenanceExpenseDto>> listExpenses(
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(billingService.listExpenses(category));
    }

    @PostMapping({"/billing/expenses", "/expenses"})
    public ApiResponse<MaintenanceExpenseDto> createExpense(
            @RequestBody MaintenanceExpenseCreateRequest request
    ) {
        MaintenanceExpenseDto created = billingService.createExpense(request);
        return ApiResponse.ok("Maintenance expense recorded with receipt attachments", created);
    }
}
