package com.aikya.domain.controller;

import com.aikya.common.ApiResponse;
import com.aikya.domain.dto.OfflinePaymentRequest;
import com.aikya.domain.dto.OnlineOrderRequest;
import com.aikya.domain.dto.OnlineOrderResponse;
import com.aikya.domain.dto.OnlinePaymentWebhookRequest;
import com.aikya.domain.dto.PaymentDto;
import com.aikya.domain.dto.ReconcilePaymentRequest;
import com.aikya.domain.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ApiResponse<List<PaymentDto>> listPayments(
            @RequestParam(required = false) String reconciliationStatus
    ) {
        return ApiResponse.ok(paymentService.listPayments(reconciliationStatus));
    }

    @PostMapping("/online/order")
    public ApiResponse<OnlineOrderResponse> createOnlineOrder(@RequestBody OnlineOrderRequest request) {
        return ApiResponse.ok("Online payment gateway order initialized", paymentService.createOnlineOrder(request));
    }

    @PostMapping("/online/webhook")
    public ApiResponse<PaymentDto> completeOnlinePayment(@RequestBody OnlinePaymentWebhookRequest request) {
        return ApiResponse.ok("Online payment verified and receipt generated", paymentService.completeOnlinePayment(request));
    }

    @PostMapping("/offline")
    public ApiResponse<PaymentDto> submitOfflinePayment(@RequestBody OfflinePaymentRequest request) {
        return ApiResponse.ok("Offline payment submitted for Treasurer verification", paymentService.submitOfflinePayment(request));
    }

    @PostMapping("/{paymentId}/reconcile")
    public ApiResponse<PaymentDto> reconcilePayment(
            @PathVariable Long paymentId,
            @RequestBody ReconcilePaymentRequest request
    ) {
        return ApiResponse.ok("Payment reconciliation completed", paymentService.reconcilePayment(paymentId, request));
    }
}
