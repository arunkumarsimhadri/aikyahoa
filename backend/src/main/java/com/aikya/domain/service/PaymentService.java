package com.aikya.domain.service;

import com.aikya.common.AikyaException;
import com.aikya.common.AuthenticatedUser;
import com.aikya.common.NotificationPort;
import com.aikya.common.SecurityContextHelper;
import com.aikya.domain.dto.OfflinePaymentRequest;
import com.aikya.domain.dto.OnlineOrderRequest;
import com.aikya.domain.dto.OnlineOrderResponse;
import com.aikya.domain.dto.OnlinePaymentWebhookRequest;
import com.aikya.domain.dto.PaymentDto;
import com.aikya.domain.dto.ReconcilePaymentRequest;
import com.aikya.domain.entity.Invoice;
import com.aikya.domain.entity.Payment;
import com.aikya.domain.entity.Unit;
import com.aikya.domain.repository.InvoiceRepository;
import com.aikya.domain.repository.PaymentRepository;
import com.aikya.domain.repository.UnitRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UnitRepository unitRepository;
    private final ObjectProvider<NotificationPort> notificationPortProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            UnitRepository unitRepository,
            ObjectProvider<NotificationPort> notificationPortProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.unitRepository = unitRepository;
        this.notificationPortProvider = notificationPortProvider;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listPayments(String reconciliationStatus) {
        List<Payment> payments;
        if (reconciliationStatus != null && !reconciliationStatus.isBlank()) {
            payments = paymentRepository.findByReconciliationStatusOrderByCreatedAtDesc(
                    reconciliationStatus.trim().toUpperCase());
        } else {
            payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        }

        Map<Long, Invoice> invoiceMap = invoiceRepository.findAll().stream()
                .collect(Collectors.toMap(Invoice::getId, Function.identity()));
        Map<Long, Unit> unitMap = unitRepository.findAll().stream()
                .collect(Collectors.toMap(Unit::getId, Function.identity()));

        return payments.stream()
                .map(p -> toPaymentDto(p, invoiceMap.get(p.getInvoiceId()), unitMap.get(p.getUnitId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OnlineOrderResponse createOnlineOrder(OnlineOrderRequest request) {
        if (request == null || request.invoiceId() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Invoice ID is required to create an online payment order");
        }
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Invoice not found: " + request.invoiceId()));

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new AikyaException(HttpStatus.CONFLICT, "Invoice is already paid in full");
        }

        BigDecimal balanceDue = invoice.getTotalAmount()
                .subtract(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        String gateway = (request.gateway() != null && !request.gateway().isBlank())
                ? request.gateway().trim().toUpperCase()
                : "RAZORPAY";
        String orderId = "order_aikya_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        return new OnlineOrderResponse(
                orderId,
                gateway,
                balanceDue,
                "INR",
                invoice.getId(),
                invoice.getInvoiceNumber()
        );
    }

    @Transactional
    public PaymentDto completeOnlinePayment(OnlinePaymentWebhookRequest request) {
        if (request == null || request.invoiceId() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Invoice ID is required");
        }
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Invoice not found: " + request.invoiceId()));
        Unit unit = unitRepository.findById(invoice.getUnitId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Unit not found: " + invoice.getUnitId()));

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUserOrNull();
        BigDecimal balanceDue = invoice.getTotalAmount()
                .subtract(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);
        BigDecimal payAmount = (request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0)
                ? request.amount().setScale(2, RoundingMode.HALF_UP)
                : balanceDue.setScale(2, RoundingMode.HALF_UP);

        String orderId = (request.gatewayOrderId() != null && !request.gatewayOrderId().isBlank())
                ? request.gatewayOrderId()
                : "order_aikya_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String paymentId = (request.gatewayPaymentId() != null && !request.gatewayPaymentId().isBlank())
                ? request.gatewayPaymentId()
                : "pay_aikya_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String gatewayName = (request.gatewayName() != null && !request.gatewayName().isBlank())
                ? request.gatewayName().trim().toUpperCase()
                : "RAZORPAY";

        Payment payment = new Payment();
        payment.setInvoiceId(invoice.getId());
        payment.setUnitId(unit.getId());
        payment.setPayerUserId(currentUser != null ? currentUser.userId() : unit.getOwnerUserId());
        payment.setPayerName(currentUser != null ? currentUser.fullName() : unit.getOwnerName());
        payment.setAmount(payAmount);
        payment.setPaymentMethod("ONLINE");
        payment.setGatewayName(gatewayName);
        payment.setGatewayOrderId(orderId);
        payment.setGatewayPaymentId(paymentId);
        payment.setReferenceNumber(paymentId);
        payment.setReconciliationStatus("VERIFIED");
        payment.setReconciledByName("GATEWAY_AUTO_WEBHOOK");
        payment.setReconciledAt(Instant.now());
        payment.setAuditNotes("Auto-reconciled via " + gatewayName + " webhook (" + paymentId + ")");
        payment.setPaidAt(Instant.now());
        payment.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        applyVerifiedPaymentToInvoice(invoice, payAmount);

        dispatchPaymentNotification(unit, currentUser, "PAYMENT_RECONCILED", Map.of(
                "unitNumber", unit.getUnitNumber(),
                "invoiceNumber", invoice.getInvoiceNumber(),
                "amount", payAmount.toPlainString(),
                "referenceNumber", paymentId,
                "decision", "VERIFIED",
                "auditNotes", "Auto-reconciled via " + gatewayName + " webhook"
        ));

        return toPaymentDto(savedPayment, invoice, unit);
    }

    @Transactional
    public PaymentDto submitOfflinePayment(OfflinePaymentRequest request) {
        if (request == null || request.invoiceId() == null) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Invoice ID is required");
        }
        if (request.referenceNumber() == null || request.referenceNumber().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST,
                    "UTR / Cheque / Transaction Reference Number is required for offline payment submission");
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Invoice not found: " + request.invoiceId()));
        Unit unit = unitRepository.findById(invoice.getUnitId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Unit not found: " + invoice.getUnitId()));

        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        BigDecimal balanceDue = invoice.getTotalAmount()
                .subtract(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);
        BigDecimal payAmount = (request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) > 0)
                ? request.amount().setScale(2, RoundingMode.HALF_UP)
                : balanceDue.setScale(2, RoundingMode.HALF_UP);

        Set<String> allUrls = new LinkedHashSet<>();
        if (request.proofFileUrls() != null) {
            for (String url : request.proofFileUrls()) {
                if (url != null && !url.isBlank()) {
                    allUrls.add(url.trim());
                }
            }
        }
        if (request.attachments() != null) {
            for (String url : request.attachments()) {
                if (url != null && !url.isBlank()) {
                    allUrls.add(url.trim());
                }
            }
        }
        if (request.proofFileUrl() != null && !request.proofFileUrl().isBlank()) {
            for (String part : request.proofFileUrl().split(",")) {
                if (!part.trim().isEmpty()) {
                    allUrls.add(part.trim());
                }
            }
        }
        List<String> normalizedUrls = new ArrayList<>(allUrls);

        Payment payment = new Payment();
        payment.setInvoiceId(invoice.getId());
        payment.setUnitId(unit.getId());
        payment.setPayerUserId(currentUser.userId());
        payment.setPayerName(currentUser.fullName());
        payment.setAmount(payAmount);
        payment.setPaymentMethod(request.paymentMethod() != null && !request.paymentMethod().isBlank()
                ? request.paymentMethod().trim().toUpperCase()
                : "NEFT");
        payment.setReferenceNumber(request.referenceNumber().trim());
        payment.setProofFileUrl(normalizedUrls.isEmpty() ? null : normalizedUrls.get(0));
        payment.setProofFileUrls(normalizedUrls.isEmpty() ? null : String.join(",", normalizedUrls));
        payment.setReconciliationStatus("PENDING_VERIFICATION");
        payment.setAuditNotes(request.notes());
        payment.setPaidAt(Instant.now());
        payment.setCreatedAt(Instant.now());

        Payment saved = paymentRepository.save(payment);
        return toPaymentDto(saved, invoice, unit);
    }

    @Transactional
    public PaymentDto reconcilePayment(Long paymentId, ReconcilePaymentRequest request) {
        AuthenticatedUser currentUser = SecurityContextHelper.getCurrentUser();
        if (!currentUser.role().canReconcilePayments()) {
            throw new AikyaException(HttpStatus.FORBIDDEN,
                    "Only Treasurer, President, or Super Admin can reconcile offline payments");
        }

        if (request == null || request.decision() == null || request.decision().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Reconciliation decision (VERIFIED or REJECTED) is required");
        }
        if (request.auditNotes() == null || request.auditNotes().isBlank()) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Treasurer audit notes are mandatory for reconciliation");
        }

        String decision = request.decision().trim().toUpperCase();
        if (!"VERIFIED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new AikyaException(HttpStatus.BAD_REQUEST, "Decision must be VERIFIED or REJECTED");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));

        if ("VERIFIED".equalsIgnoreCase(payment.getReconciliationStatus())) {
            throw new AikyaException(HttpStatus.CONFLICT, "Payment has already been verified");
        }

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Invoice not found: " + payment.getInvoiceId()));
        Unit unit = unitRepository.findById(payment.getUnitId())
                .orElseThrow(() -> new AikyaException(HttpStatus.NOT_FOUND, "Unit not found: " + payment.getUnitId()));

        payment.setReconciliationStatus(decision);
        payment.setReconciledByUserId(currentUser.userId());
        payment.setReconciledByName(currentUser.fullName());
        payment.setReconciledAt(Instant.now());
        payment.setAuditNotes(request.auditNotes().trim());

        Payment savedPayment = paymentRepository.save(payment);

        if ("VERIFIED".equals(decision)) {
            applyVerifiedPaymentToInvoice(invoice, payment.getAmount());
            dispatchPaymentNotification(unit, currentUser, "PAYMENT_RECONCILED", Map.of(
                    "unitNumber", unit.getUnitNumber(),
                    "invoiceNumber", invoice.getInvoiceNumber(),
                    "amount", payment.getAmount().toPlainString(),
                    "referenceNumber", payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "N/A",
                    "decision", "VERIFIED",
                    "auditNotes", request.auditNotes().trim()
            ));
        } else {
            dispatchPaymentNotification(unit, currentUser, "PAYMENT_RECONCILED", Map.of(
                    "unitNumber", unit.getUnitNumber(),
                    "invoiceNumber", invoice.getInvoiceNumber(),
                    "amount", payment.getAmount().toPlainString(),
                    "referenceNumber", payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "N/A",
                    "decision", "REJECTED",
                    "auditNotes", request.auditNotes().trim()
            ));
        }

        return toPaymentDto(savedPayment, invoice, unit);
    }

    private void applyVerifiedPaymentToInvoice(Invoice invoice, BigDecimal paymentAmount) {
        BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal updatedPaid = currentPaid.add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        invoice.setPaidAmount(updatedPaid);
        if (updatedPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus("PAID");
        } else if (updatedPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        }
        invoiceRepository.save(invoice);
    }

    private void dispatchPaymentNotification(Unit unit, AuthenticatedUser actor, String templateKey, Map<String, Object> vars) {
        NotificationPort notificationPort = notificationPortProvider.getIfAvailable();
        if (notificationPort != null && unit.getOwnerPhone() != null && !unit.getOwnerPhone().isBlank()) {
            String locale = (actor != null && actor.preferredLocale() != null) ? actor.preferredLocale() : "en";
            notificationPort.sendNotification(
                    unit.getOwnerUserId(),
                    unit.getOwnerPhone(),
                    unit.getOwnerName() != null ? unit.getOwnerName() : "Resident",
                    locale,
                    "SMS",
                    templateKey,
                    vars
            );
        }
    }

    public PaymentDto toPaymentDto(Payment p, Invoice invoice, Unit unit) {
        List<String> urls = new ArrayList<>();
        if (p.getProofFileUrls() != null && !p.getProofFileUrls().isBlank()) {
            urls.addAll(Arrays.stream(p.getProofFileUrls().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        } else if (p.getProofFileUrl() != null && !p.getProofFileUrl().isBlank()) {
            urls.add(p.getProofFileUrl().trim());
        }
        String primaryUrl = p.getProofFileUrl() != null ? p.getProofFileUrl() : (urls.isEmpty() ? null : urls.get(0));

        return new PaymentDto(
                p.getId(),
                p.getInvoiceId(),
                invoice != null ? invoice.getInvoiceNumber() : "INV-" + p.getInvoiceId(),
                p.getUnitId(),
                unit != null ? unit.getUnitNumber() : "Unit-" + p.getUnitId(),
                p.getPayerUserId(),
                p.getPayerName(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getGatewayName(),
                p.getGatewayOrderId(),
                p.getGatewayPaymentId(),
                p.getReferenceNumber(),
                primaryUrl,
                urls,
                urls,
                p.getReconciliationStatus(),
                p.getReconciledByUserId(),
                p.getReconciledByName(),
                p.getReconciledAt(),
                p.getAuditNotes(),
                p.getPaidAt(),
                p.getCreatedAt()
        );
    }
}
