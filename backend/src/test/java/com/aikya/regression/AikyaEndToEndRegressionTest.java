package com.aikya.regression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End Regression Test Suite for Project Aikya (ఐక్య - Indian Apartment & Community Management Platform).
 * Primary Association: Elite Towers (ఏలైట్ టవర్స్), Ongole, Andhra Pradesh (523001) - Schema: aikya_assoc_1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AikyaEndToEndRegressionTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTH_HEADER = "Authorization";
    private static final String TENANT_ELITE_TOWERS = "aikya_assoc_1";
    private static final String TENANT_SECOND_ASSOC = "aikya_assoc_2";

    private static final String TREASURER_PHONE = "+919848011111";
    private static final String OWNER_PHONE = "+919848022222";
    private static final String TENANT_PHONE = "+919848033333";

    private static String treasurerToken;
    private static String ownerToken;
    private static String tenantToken;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("Test 1: Association Onboarding & Dynamic Schema Provisioning (Elite Towers Ongole & Multi-RWA)")
    void test01_associationOnboardingAndDynamicSchemaProvisioning() throws Exception {
        // 1. Verify seeded primary association ("Elite Towers" in Ongole, AP 523001 -> aikya_assoc_1)
        // and secondary association ("Sri Sai Aikya Towers RWA" -> aikya_assoc_2)
        MvcResult listResult = mockMvc.perform(get("/api/v1/associations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode listData = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data");
        assertThat(listData.isArray()).isTrue();
        assertThat(listData.size()).isGreaterThanOrEqualTo(2);

        List<String> schemas = new ArrayList<>();
        boolean foundEliteTowersOngole = false;
        for (JsonNode assoc : listData) {
            String schemaName = assoc.path("schemaName").asText();
            schemas.add(schemaName);
            assertThat(assoc.path("active").asBoolean()).isTrue();
            if (TENANT_ELITE_TOWERS.equals(schemaName)) {
                foundEliteTowersOngole = true;
                assertThat(assoc.path("name").asText()).contains("Elite Towers");
                assertThat(assoc.path("city").asText()).isEqualToIgnoringCase("Ongole");
                assertThat(assoc.path("state").asText()).isEqualToIgnoringCase("Andhra Pradesh");
                assertThat(assoc.path("pincode").asText()).isEqualTo("523001");
            }
        }
        assertThat(foundEliteTowersOngole).isTrue();
        assertThat(schemas).contains(TENANT_ELITE_TOWERS, TENANT_SECOND_ASSOC);

        // 2. Dynamically onboard a new association ("Cyber Meadows RWA") and verify new schema provisioning
        Map<String, Object> createPayload = Map.of(
                "name", "Cyber Meadows RWA",
                "nameTe", "సైబర్ మెడోస్ అసోసియేషన్",
                "code", "CYBER_MEADOWS_RWA",
                "address", "Plot 42, Kurnool Road, Ongole",
                "city", "Ongole",
                "state", "Andhra Pradesh",
                "pincode", "523001",
                "billingType", "AREA_BASED",
                "ratePerSqft", new BigDecimal("3.50"),
                "sinkingFundAmount", new BigDecimal("500.00")
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/associations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Cyber Meadows RWA"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        JsonNode createdAssoc = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        String provisionedSchema = createdAssoc.path("schemaName").asText();
        assertThat(provisionedSchema)
                .startsWith("aikya_assoc_")
                .isNotEqualTo(TENANT_ELITE_TOWERS)
                .isNotEqualTo(TENANT_SECOND_ASSOC);
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Passwordless Phone + OTP Authentication & Bilingual Locale Switching (en / te)")
    void test02_passwordlessPhoneOtpAuthenticationAndLocaleSwitching() throws Exception {
        treasurerToken = authenticateViaOtp(TREASURER_PHONE, "TREASURER");
        ownerToken = authenticateViaOtp(OWNER_PHONE, "OWNER");
        tenantToken = authenticateViaOtp(TENANT_PHONE, "TENANT");

        assertThat(treasurerToken).isNotBlank();
        assertThat(ownerToken).isNotBlank();
        assertThat(tenantToken).isNotBlank();

        // Switch Owner preferred locale to Telugu ("te")
        MvcResult localeResult = mockMvc.perform(patch("/api/v1/users/locale")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("preferredLocale", "te"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.preferredLocale").value("te"))
                .andReturn();

        JsonNode userNode = objectMapper.readTree(localeResult.getResponse().getContentAsString()).path("data");
        assertThat(userNode.path("preferredLocale").asText()).isEqualTo("te");

        // Verify /api/v1/auth/me reflects updated Telugu locale
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredLocale").value("te"));
    }

    private String authenticateViaOtp(String phoneNumber, String expectedRole) throws Exception {
        MvcResult otpSendResult = mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phoneNumber", phoneNumber))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode sendData = objectMapper.readTree(otpSendResult.getResponse().getContentAsString()).path("data");
        String otpCode = sendData.path("mockOtpCode").asText("123456");
        if (otpCode == null || otpCode.isBlank()) {
            otpCode = "123456";
        }

        MvcResult verifyResult = mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phoneNumber", phoneNumber,
                                "otpCode", otpCode
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode verifyData = objectMapper.readTree(verifyResult.getResponse().getContentAsString()).path("data");
        assertThat(verifyData.path("user").path("role").asText()).isEqualTo(expectedRole);
        return verifyData.path("accessToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Multi-Tenant Schema Isolation Regression (aikya_assoc_1 vs aikya_assoc_2)")
    void test03_multiTenantSchemaIsolationRegression() throws Exception {
        // Publish a notice inside Tenant 1 (Elite Towers Ongole - aikya_assoc_1)
        String exclusiveTitle = "Elite Towers Ongole Exclusive Borewell Notice #" + System.currentTimeMillis();
        Map<String, Object> noticePayload = Map.of(
                "title", exclusiveTitle,
                "titleTe", "ఏలైట్ టవర్స్ ఒంగోలు ప్రత్యేక ప్రకటన",
                "content", "Exclusive notice for Elite Towers Block A & B residents in Ongole.",
                "contentTe", "ఏలైట్ టవర్స్ నివాసితులకు మాత్రమే ప్రత్యేక ప్రకటన.",
                "category", "MAINTENANCE",
                "priority", "HIGH",
                "pinned", true
        );

        mockMvc.perform(post("/api/v1/notices")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noticePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify notice is present in Tenant 1 (aikya_assoc_1)
        MvcResult tenant1NoticesRes = mockMvc.perform(get("/api/v1/notices")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();
        String tenant1Body = tenant1NoticesRes.getResponse().getContentAsString();
        assertThat(tenant1Body).contains(exclusiveTitle);

        // Verify notice NEVER leaks into Tenant 2 (aikya_assoc_2)
        MvcResult tenant2NoticesRes = mockMvc.perform(get("/api/v1/notices")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_SECOND_ASSOC))
                .andExpect(status().isOk())
                .andReturn();
        String tenant2Body = tenant2NoticesRes.getResponse().getContentAsString();
        assertThat(tenant2Body).doesNotContain(exclusiveTitle);

        // Also verify Unit isolation between aikya_assoc_1 (A-101, A-102, B-201) and aikya_assoc_2 (C-101)
        MvcResult tenant1UnitsRes = mockMvc.perform(get("/api/v1/units")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tenant1UnitsRes.getResponse().getContentAsString())
                .contains("A-101", "A-102", "B-201")
                .doesNotContain("C-101");

        MvcResult tenant2UnitsRes = mockMvc.perform(get("/api/v1/units")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_SECOND_ASSOC))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(tenant2UnitsRes.getResponse().getContentAsString())
                .contains("C-101")
                .doesNotContain("A-101", "A-102");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Monthly Maintenance Billing Engine (AREA_BASED Calculation & RBAC)")
    void test04_monthlyMaintenanceBillingEngineAreaBasedAndRbac() throws Exception {
        Map<String, Object> billingRunPayload = Map.of(
                "billingMonth", 12,
                "billingYear", 2026,
                "dueDate", "2026-12-10",
                "notes", "Dec 2026 Maintenance Billing Run - Elite Towers Ongole"
        );

        // 1. Verify TENANT role is forbidden from executing a billing run (403 Forbidden)
        mockMvc.perform(post("/api/v1/billing/runs")
                        .header(AUTH_HEADER, "Bearer " + tenantToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billingRunPayload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 2. Verify TREASURER role executes billing run successfully (200 OK)
        MvcResult runResult = mockMvc.perform(post("/api/v1/billing/runs")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(billingRunPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode invoices = objectMapper.readTree(runResult.getResponse().getContentAsString()).path("data");
        assertThat(invoices.isArray()).isTrue();
        assertThat(invoices.size()).isGreaterThanOrEqualTo(4);

        // Verify AREA_BASED formula: maintenanceCharge = areaSqft * 3.50 + sinkingFund (500.00) + waterCharge (450.00)
        // Unit A-101 (1600 sqft): 1600 * 3.50 = 5600.00; + 500 + 450 = 6550.00
        // Unit A-102 (1450 sqft): 1450 * 3.50 = 5075.00; + 500 + 450 = 6025.00
        boolean verifiedA101 = false;
        boolean verifiedA102 = false;
        for (JsonNode inv : invoices) {
            String unitNumber = inv.path("unitNumber").asText();
            BigDecimal maintenance = new BigDecimal(inv.path("maintenanceCharge").asText());
            BigDecimal sinkingFund = new BigDecimal(inv.path("sinkingFundCharge").asText());
            BigDecimal waterCharge = new BigDecimal(inv.path("waterCharge").asText());
            BigDecimal totalAmount = new BigDecimal(inv.path("totalAmount").asText());

            assertThat(sinkingFund).isEqualByComparingTo("500.00");
            assertThat(waterCharge).isEqualByComparingTo("450.00");
            assertThat(totalAmount).isEqualByComparingTo(maintenance.add(sinkingFund).add(waterCharge));

            if ("A-101".equals(unitNumber)) {
                assertThat(maintenance).isEqualByComparingTo("5600.00");
                assertThat(totalAmount).isEqualByComparingTo("6550.00");
                verifiedA101 = true;
            } else if ("A-102".equals(unitNumber)) {
                assertThat(maintenance).isEqualByComparingTo("5075.00");
                assertThat(totalAmount).isEqualByComparingTo("6025.00");
                verifiedA102 = true;
            }
        }
        assertThat(verifiedA101).isTrue();
        assertThat(verifiedA102).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Online Payment Webhook & Offline Payment Treasurer Reconciliation Workflow")
    void test05_onlinePaymentWebhookAndOfflinePaymentTreasurerReconciliation() throws Exception {
        // Fetch PENDING invoices in Elite Towers (aikya_assoc_1)
        MvcResult invoicesRes = mockMvc.perform(get("/api/v1/billing/invoices")
                        .param("status", "PENDING")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode pendingInvoices = objectMapper.readTree(invoicesRes.getResponse().getContentAsString()).path("data");
        assertThat(pendingInvoices.size()).isGreaterThanOrEqualTo(2);

        JsonNode targetOfflineInvoice = pendingInvoices.get(0);
        long offlineInvoiceId = targetOfflineInvoice.path("id").asLong();
        BigDecimal offlineAmount = new BigDecimal(targetOfflineInvoice.path("totalAmount").asText());

        // 1. Resident submits an offline NEFT payment with transaction proof URLs
        Map<String, Object> offlineSubmitPayload = Map.of(
                "invoiceId", offlineInvoiceId,
                "amount", offlineAmount,
                "paymentMethod", "NEFT",
                "referenceNumber", "HDFC000998877",
                "proofFileUrl", "http://192.168.10.83/aikya-storage/aikya_assoc_1/neft-hdfc000998877.pdf",
                "proofFileUrls", List.of(
                        "http://192.168.10.83/aikya-storage/aikya_assoc_1/neft-hdfc000998877.pdf",
                        "http://192.168.10.83/aikya-storage/aikya_assoc_1/neft-screenshot-hdfc000998877.png"
                ),
                "notes", "NEFT transfer from HDFC Ongole branch"
        );

        MvcResult offlineSubmitRes = mockMvc.perform(post("/api/v1/payments/offline")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineSubmitPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reconciliationStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.referenceNumber").value("HDFC000998877"))
                .andReturn();

        long paymentId = objectMapper.readTree(offlineSubmitRes.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        Map<String, Object> reconcilePayload = Map.of(
                "decision", "VERIFIED",
                "auditNotes", "Verified against SBI RWA Current Account statement"
        );

        // 2. Verify OWNER and TENANT cannot reconcile offline payments (403 Forbidden)
        mockMvc.perform(post("/api/v1/payments/{paymentId}/reconcile", paymentId)
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reconcilePayload)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/payments/{paymentId}/reconcile", paymentId)
                        .header(AUTH_HEADER, "Bearer " + tenantToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reconcilePayload)))
                .andExpect(status().isForbidden());

        // 3. Verify TREASURER reconciles payment -> status becomes VERIFIED and invoice becomes PAID
        mockMvc.perform(post("/api/v1/payments/{paymentId}/reconcile", paymentId)
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reconcilePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reconciliationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.auditNotes").value("Verified against SBI RWA Current Account statement"));

        // Confirm invoice status is now PAID
        MvcResult verifyInvoiceRes = mockMvc.perform(get("/api/v1/billing/invoices")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode allInvoices = objectMapper.readTree(verifyInvoiceRes.getResponse().getContentAsString()).path("data");
        boolean foundPaidInvoice = false;
        for (JsonNode inv : allInvoices) {
            if (inv.path("id").asLong() == offlineInvoiceId) {
                assertThat(inv.path("status").asText()).isEqualTo("PAID");
                foundPaidInvoice = true;
            }
        }
        assertThat(foundPaidInvoice).isTrue();

        // 4. Verify Online Payment Order & Webhook on second pending invoice
        JsonNode targetOnlineInvoice = pendingInvoices.get(1);
        long onlineInvoiceId = targetOnlineInvoice.path("id").asLong();

        MvcResult orderRes = mockMvc.perform(post("/api/v1/payments/online/order")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "invoiceId", onlineInvoiceId,
                                "gateway", "RAZORPAY"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").isNotEmpty())
                .andReturn();

        String gatewayOrderId = objectMapper.readTree(orderRes.getResponse().getContentAsString())
                .path("data").path("orderId").asText();

        mockMvc.perform(post("/api/v1/payments/online/webhook")
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "invoiceId", onlineInvoiceId,
                                "gatewayName", "RAZORPAY",
                                "gatewayOrderId", gatewayOrderId,
                                "gatewayPaymentId", "pay_razorpay_et_998877"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reconciliationStatus").value("VERIFIED"));
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Helpdesk Ticket Lifecycle (OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED) & SLA Tracking")
    void test06_helpdeskTicketLifecycleAndSlaTracking() throws Exception {
        // 1. Resident raises a PLUMBING ticket with HIGH priority
        Map<String, Object> createTicketPayload = Map.of(
                "category", "PLUMBING",
                "priority", "HIGH",
                "title", "Main bathroom overhead supply pipe leakage in A-102",
                "description", "Continuous water dripping near concealed flush valve in Master Bedroom bathroom.",
                "attachmentUrl", "http://192.168.10.83/aikya-storage/aikya_assoc_1/plumbing-leak-a102.jpg"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/tickets")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTicketPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.slaDueAt").isNotEmpty())
                .andReturn();

        JsonNode createdTicket = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data");
        long ticketId = createdTicket.path("id").asLong();
        assertThat(createdTicket.path("slaDueAt").asText()).isNotBlank();

        // 2. Transition OPEN -> IN_PROGRESS and assign technician ("Ramesh Kumar", "+919848099999")
        mockMvc.perform(patch("/api/v1/tickets/{ticketId}/status", ticketId)
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "IN_PROGRESS",
                                "assignedTechnicianName", "Ramesh Kumar",
                                "assignedTechnicianPhone", "+919848099999"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignedTechnicianName").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.data.assignedTechnicianPhone").value("+919848099999"));

        // 3. Transition IN_PROGRESS -> RESOLVED
        mockMvc.perform(patch("/api/v1/tickets/{ticketId}/status", ticketId)
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED",
                                "resolutionNotes", "Replaced brass concealed valve cartridge and teflon seal"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        // 4. Transition RESOLVED -> CLOSED
        mockMvc.perform(patch("/api/v1/tickets/{ticketId}/status", ticketId)
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CLOSED",
                                "resolutionNotes", "Confirmed leak-free with flat owner Lakshmi Narayana"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Amenity Booking Slot-Conflict Resolver")
    void test07_amenityBookingSlotConflictResolver() throws Exception {
        // Fetch Clubhouse amenity ID
        MvcResult amenitiesRes = mockMvc.perform(get("/api/v1/amenities")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode amenities = objectMapper.readTree(amenitiesRes.getResponse().getContentAsString()).path("data");
        assertThat(amenities.isArray()).isTrue();
        assertThat(amenities.size()).isGreaterThanOrEqualTo(1);
        long clubhouseId = amenities.get(0).path("id").asLong();

        // 1. Book Clubhouse on 2026-11-15 from 18:00 to 20:00 -> 200 OK
        Map<String, Object> firstBooking = Map.of(
                "amenityId", clubhouseId,
                "bookingDate", "2026-11-15",
                "startTime", "18:00",
                "endTime", "20:00",
                "purpose", "Resident Anniversary Meet at Elite Towers",
                "guestCount", 30
        );

        mockMvc.perform(post("/api/v1/amenities/bookings")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBooking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // 2. Attempt overlapping booking on 2026-11-15 from 19:00 to 21:00 -> 409 Conflict
        Map<String, Object> overlappingBooking = Map.of(
                "amenityId", clubhouseId,
                "bookingDate", "2026-11-15",
                "startTime", "19:00",
                "endTime", "21:00",
                "purpose", "Overlapping Slot Attempt",
                "guestCount", 15
        );

        mockMvc.perform(post("/api/v1/amenities/bookings")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingBooking)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // 3. Book non-overlapping adjacent slot on 2026-11-15 from 20:00 to 22:00 -> 200 OK
        Map<String, Object> nonOverlappingBooking = Map.of(
                "amenityId", clubhouseId,
                "bookingDate", "2026-11-15",
                "startTime", "20:00",
                "endTime", "22:00",
                "purpose", "Late Evening Cultural Practice",
                "guestCount", 20
        );

        mockMvc.perform(post("/api/v1/amenities/bookings")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonOverlappingBooking)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startTime").value("20:00"))
                .andExpect(jsonPath("$.data.endTime").value("22:00"));
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Governance Polls (Strict 1-Vote-Per-Unit, OWNER-Only Enforcement & Secret Ballot Anonymity)")
    void test08_governancePollsOneVotePerUnitOwnerOnlyAndSecretBallot() throws Exception {
        // 1. Create an ownerOnly=true, isSecret=true poll
        Map<String, Object> createPollPayload = Map.of(
                "title", "Approval of CCTV AI Upgrade at Elite Towers Entry Gates",
                "titleTe", "ఏలైట్ టవర్స్ ప్రధాన గేట్ల వద్ద AI సీసీటీవీ కెమెరాల ఆమోదం",
                "description", "Secret ballot resolution for flat owners (1 vote per unit).",
                "descriptionTe", "ప్రతి ఫ్లాట్‌కు ఒక ఓటు మాత్రమే (రహస్య ఓటింగ్).",
                "isSecret", true,
                "ownerOnly", true,
                "endsAt", Instant.now().plus(10, ChronoUnit.DAYS).toString(),
                "options", List.of(
                        Map.of(
                                "optionText", "Approve Upgrade (Rs. 1.2 Lakhs)",
                                "optionTextTe", "ఆమోదించండి (రూ. 1.2 లక్షలు)"
                        ),
                        Map.of(
                                "optionText", "Defer to Next AGM",
                                "optionTextTe", "తదుపరి సమావేశానికి వాయిదా"
                        )
                )
        );

        MvcResult createPollRes = mockMvc.perform(post("/api/v1/governance/polls")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPollPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSecret").value(true))
                .andExpect(jsonPath("$.data.ownerOnly").value(true))
                .andReturn();

        JsonNode createdPoll = objectMapper.readTree(createPollRes.getResponse().getContentAsString()).path("data");
        long pollId = createdPoll.path("id").asLong();
        long firstOptionId = createdPoll.path("options").get(0).path("id").asLong();

        // 2. Verify TENANT attempting to vote receives 403 Forbidden
        mockMvc.perform(post("/api/v1/governance/polls/{pollId}/vote", pollId)
                        .header(AUTH_HEADER, "Bearer " + tenantToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", firstOptionId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 3. Verify OWNER casting first vote for unit A-102 receives 200 OK
        mockMvc.perform(post("/api/v1/governance/polls/{pollId}/vote", pollId)
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", firstOptionId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalVotes").value(1));

        // 4. Verify duplicate vote for the same unit A-102 receives 409 Conflict
        mockMvc.perform(post("/api/v1/governance/polls/{pollId}/vote", pollId)
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", firstOptionId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // 5. Verify GET /api/v1/governance/polls hides voter names (voters list is empty) because isSecret == true
        MvcResult pollsListRes = mockMvc.perform(get("/api/v1/governance/polls")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode polls = objectMapper.readTree(pollsListRes.getResponse().getContentAsString()).path("data");
        boolean verifiedSecretAnonymity = false;
        for (JsonNode poll : polls) {
            if (poll.path("id").asLong() == pollId) {
                assertThat(poll.path("isSecret").asBoolean()).isTrue();
                for (JsonNode opt : poll.path("options")) {
                    assertThat(opt.path("voters").isArray()).isTrue();
                    assertThat(opt.path("voters").size()).isZero();
                }
                verifiedSecretAnonymity = true;
            }
        }
        assertThat(verifiedSecretAnonymity).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Localized Notification Engine (en and te Telugu Rendering)")
    void test09_localizedNotificationEngineEnglishAndTeluguRendering() throws Exception {
        Map<String, Map<String, Object>> templateVariables = Map.of(
                "OTP_LOGIN", Map.of("otpCode", "654321"),
                "INVOICE_GENERATED", Map.of(
                        "unitNumber", "A-102",
                        "invoiceNumber", "INV-ET-202612-A102",
                        "amount", "6025.00",
                        "dueDate", "2026-12-10"
                ),
                "PAYMENT_RECONCILED", Map.of(
                        "unitNumber", "A-102",
                        "amount", "6025.00",
                        "referenceNumber", "HDFC000998877",
                        "decision", "VERIFIED",
                        "auditNotes", "Verified against SBI RWA Current Account statement"
                ),
                "TICKET_STATUS_UPDATED", Map.of(
                        "ticketNumber", "TKT-ET-1001",
                        "category", "PLUMBING",
                        "status", "RESOLVED",
                        "technician", "Ramesh Kumar"
                )
        );

        for (Map.Entry<String, Map<String, Object>> entry : templateVariables.entrySet()) {
            String templateKey = entry.getKey();
            Map<String, Object> vars = entry.getValue();

            // English preview
            MvcResult enRes = mockMvc.perform(post("/api/v1/notifications/preview")
                            .header(AUTH_HEADER, "Bearer " + treasurerToken)
                            .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "templateKey", templateKey,
                                    "locale", "en",
                                    "channel", "SMS",
                                    "recipientPhone", OWNER_PHONE,
                                    "variables", vars
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.locale").value("en"))
                    .andReturn();

            String enMessage = objectMapper.readTree(enRes.getResponse().getContentAsString())
                    .path("data").path("renderedMessage").asText();
            assertThat(enMessage).contains("[Aikya RWA]");

            // Telugu preview
            MvcResult teRes = mockMvc.perform(post("/api/v1/notifications/preview")
                            .header(AUTH_HEADER, "Bearer " + treasurerToken)
                            .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "templateKey", templateKey,
                                    "locale", "te",
                                    "channel", "WHATSAPP",
                                    "recipientPhone", OWNER_PHONE,
                                    "variables", vars
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.locale").value("te"))
                    .andReturn();

            String teMessage = objectMapper.readTree(teRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("data").path("renderedMessage").asText();
            assertThat(teMessage).contains("ఐక్య");
            for (Object val : vars.values()) {
                assertThat(teMessage).contains(String.valueOf(val));
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test 10: Multi-File Receipt & Transaction Proof Uploads for Apartment Maintenance Records & Society Expenses")
    void test10_multiFileReceiptUploadsAndApartmentMaintenanceExpenseRecords() throws Exception {
        // 1. Upload multiple receipt/transaction proof files via multipart/form-data to /api/v1/storage/upload-multiple
        MockMultipartFile invoicePdf = new MockMultipartFile(
                "files",
                "elite-towers-borewell-winding-invoice.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Elite Towers Ongole - Borewell Submersible Pump Winding Tax Invoice #INV-902".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile utrProofPng = new MockMultipartFile(
                "files",
                "elite-towers-sbi-neft-utr-proof.png",
                MediaType.IMAGE_PNG_VALUE,
                "SBI Ongole Main Branch NEFT UTR SBIN0026119988 Rs 14800".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult multiUploadRes = mockMvc.perform(multipart("/api/v1/storage/upload-multiple")
                        .file(invoicePdf)
                        .file(utrProofPng)
                        .param("category", "ASSOCIATION_EXPENSE")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode uploadedFiles = objectMapper.readTree(multiUploadRes.getResponse().getContentAsString()).path("data");
        assertThat(uploadedFiles.isArray()).isTrue();
        assertThat(uploadedFiles.size()).isEqualTo(2);

        List<String> uploadedUrls = new ArrayList<>();
        for (JsonNode fileNode : uploadedFiles) {
            String url = fileNode.path("fileUrl").asText();
            assertThat(url).isNotBlank().contains(TENANT_ELITE_TOWERS);
            uploadedUrls.add(url);
        }

        // 2. Verify TENANT cannot create society maintenance expense records (403 Forbidden)
        Map<String, Object> expensePayload = Map.ofEntries(
                Map.entry("category", "BOREWELL_PUMP"),
                Map.entry("title", "Submersible Borewell Motor Rewinding & Pump Repair (Block A)"),
                Map.entry("titleTe", "బోరువెల్ మోటార్ రిపేరు మరియు వైండింగ్ ఖర్చు (బ్లాక్ A)"),
                Map.entry("vendorName", "Sri Lakshmi Electrical & Engineering Works, Ongole"),
                Map.entry("amount", new BigDecimal("14800.00")),
                Map.entry("expenseDate", "2026-11-05"),
                Map.entry("paymentMode", "NEFT"),
                Map.entry("transactionReference", "UTR-SBIN0026119988"),
                Map.entry("receiptFileUrls", uploadedUrls),
                Map.entry("attachments", uploadedUrls),
                Map.entry("approvedByName", "Padmavathi Devi (President)"),
                Map.entry("notes", "Emergency borewell pump repair with tax invoice and SBI NEFT receipt proofs")
        );

        mockMvc.perform(post("/api/v1/billing/expenses")
                        .header(AUTH_HEADER, "Bearer " + tenantToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensePayload)))
                .andExpect(status().isForbidden());

        // 3. Verify TREASURER records maintenance expense with multiple receipt attachments (200 OK)
        MvcResult createExpenseRes = mockMvc.perform(post("/api/v1/billing/expenses")
                        .header(AUTH_HEADER, "Bearer " + treasurerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("BOREWELL_PUMP"))
                .andExpect(jsonPath("$.data.transactionReference").value("UTR-SBIN0026119988"))
                .andReturn();

        JsonNode createdExpense = objectMapper.readTree(createExpenseRes.getResponse().getContentAsString()).path("data");
        assertThat(createdExpense.path("receiptFileUrls").isArray()).isTrue();
        assertThat(createdExpense.path("receiptFileUrls").size()).isEqualTo(2);

        // 4. Verify GET /api/v1/billing/expenses returns recorded and seeded Elite Towers Ongole expenses with proofs
        MvcResult listExpensesRes = mockMvc.perform(get("/api/v1/billing/expenses")
                        .header(AUTH_HEADER, "Bearer " + ownerToken)
                        .header(TENANT_HEADER, TENANT_ELITE_TOWERS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode expensesList = objectMapper.readTree(listExpensesRes.getResponse().getContentAsString()).path("data");
        assertThat(expensesList.isArray()).isTrue();
        assertThat(expensesList.size()).isGreaterThanOrEqualTo(4);

        // 5. Verify stored file download endpoint serves uploaded receipt file bytes
        mockMvc.perform(get("/api/v1/storage/files/{tenant}/{filename}",
                        TENANT_ELITE_TOWERS, "elite-towers-borewell-winding-invoice.pdf"))
                .andExpect(status().isOk());
    }
}
