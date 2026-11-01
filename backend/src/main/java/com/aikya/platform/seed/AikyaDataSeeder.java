package com.aikya.platform.seed;

import com.aikya.common.BillingType;
import com.aikya.common.Role;
import com.aikya.common.TenantContext;
import com.aikya.platform.entity.Association;
import com.aikya.platform.entity.AssociationMembership;
import com.aikya.platform.entity.User;
import com.aikya.platform.multitenancy.TenantMigrationService;
import com.aikya.platform.repository.AssociationMembershipRepository;
import com.aikya.platform.repository.AssociationRepository;
import com.aikya.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class AikyaDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AikyaDataSeeder.class);

    private final TenantMigrationService tenantMigrationService;
    private final AssociationRepository associationRepository;
    private final UserRepository userRepository;
    private final AssociationMembershipRepository membershipRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${aikya.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${aikya.storage.minio.endpoint:http://192.168.10.83}")
    private String minioEndpoint;

    @Value("${aikya.storage.minio.bucket:aikya-storage}")
    private String minioBucket;

    public AikyaDataSeeder(TenantMigrationService tenantMigrationService,
                           AssociationRepository associationRepository,
                           UserRepository userRepository,
                           AssociationMembershipRepository membershipRepository,
                           JdbcTemplate jdbcTemplate) {
        this.tenantMigrationService = tenantMigrationService;
        this.associationRepository = associationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantMigrationService.migratePublicSchema();
        if (!seedEnabled) {
            return;
        }
        try {
            TenantContext.setCurrentTenant(TenantContext.DEFAULT_TENANT);
            seedPublicAndTenantSchemas();
        } catch (Exception ex) {
            log.warn("Seed check completed with note: {}", ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    public void seedPublicAndTenantSchemas() {
        Association eliteTowers = associationRepository.findByCode("ELITE_TOWERS_ONGOLE").orElseGet(() -> {
            Association a = new Association();
            a.setName("Elite Towers");
            a.setNameTe("ఏలైట్ టవర్స్, ఒంగోలు");
            a.setCode("ELITE_TOWERS_ONGOLE");
            a.setSchemaName("aikya_assoc_1");
            a.setAddress("Kurnool Road, Near Collectorate, Ongole");
            a.setCity("Ongole");
            a.setState("Andhra Pradesh");
            a.setPincode("523001");
            a.setBillingType(BillingType.AREA_BASED);
            a.setRatePerSqft(new BigDecimal("3.50"));
            a.setFlatMaintenanceAmount(new BigDecimal("3500.00"));
            a.setSinkingFundAmount(new BigDecimal("500.00"));
            a.setWaterChargePerUnit(new BigDecimal("450.00"));
            a.setLateFeePercentage(new BigDecimal("2.00"));
            a.setActive(true);
            return associationRepository.save(a);
        });

        Association sriSai = associationRepository.findByCode("SRI_SAI_AIKYA").orElseGet(() -> {
            Association a = new Association();
            a.setName("Sri Sai Aikya Towers RWA");
            a.setNameTe("శ్రీ సాయి ఐక్య టవర్స్ RWA");
            a.setCode("SRI_SAI_AIKYA");
            a.setSchemaName("aikya_assoc_2");
            a.setAddress("VIP Road, Lawyerpet, Ongole");
            a.setCity("Ongole");
            a.setState("Andhra Pradesh");
            a.setPincode("523002");
            a.setBillingType(BillingType.FLAT_RATE);
            a.setRatePerSqft(new BigDecimal("3.00"));
            a.setFlatMaintenanceAmount(new BigDecimal("4000.00"));
            a.setSinkingFundAmount(new BigDecimal("600.00"));
            a.setWaterChargePerUnit(new BigDecimal("400.00"));
            a.setLateFeePercentage(new BigDecimal("2.00"));
            a.setActive(true);
            return associationRepository.save(a);
        });

        for (Association assoc : associationRepository.findAll()) {
            tenantMigrationService.provisionAndMigrateTenantSchema(assoc.getSchemaName());
        }

        User treasurer = ensureUser("+919848011111", "Venkata Subba Rao", "వెంకట సుబ్బారావు", "treasurer@elitetowers.in", "te");
        User owner = ensureUser("+919848022222", "Lakshmi Narayana", "లక్ష్మీ నారాయణ", "lakshmi@elitetowers.in", "en");
        User tenant = ensureUser("+919848033333", "Srinivasulu Reddy", "శ్రీనివాసులు రెడ్డి", "srinivas@elitetowers.in", "te");
        User president = ensureUser("+919848044444", "Padmavathi Devi", "పద్మావతి దేవి", "president@elitetowers.in", "te");

        ensureMembership(treasurer.getId(), eliteTowers.getId(), Role.TREASURER, "A-101", "A");
        ensureMembership(owner.getId(), eliteTowers.getId(), Role.OWNER, "A-102", "A");
        ensureMembership(tenant.getId(), eliteTowers.getId(), Role.TENANT, "B-201", "B");
        ensureMembership(president.getId(), eliteTowers.getId(), Role.PRESIDENT, "A-103", "A");
        ensureMembership(president.getId(), sriSai.getId(), Role.PRESIDENT, "C-101", "C");

        seedEliteTowersTenant(eliteTowers.getSchemaName(), treasurer, owner, tenant, president);
        seedSriSaiTenant(sriSai.getSchemaName(), president);
    }

    private User ensureUser(String phone, String name, String nameTe, String email, String locale) {
        return userRepository.findByPhoneNumber(phone).orElseGet(() -> {
            User u = new User();
            u.setPhoneNumber(phone);
            u.setFullName(name);
            u.setFullNameTe(nameTe);
            u.setEmail(email);
            u.setPreferredLocale(locale);
            u.setActive(true);
            return userRepository.save(u);
        });
    }

    private void ensureMembership(Long userId, Long associationId, Role role, String unitNumber, String blockName) {
        boolean exists = membershipRepository.findByUserIdAndActiveTrue(userId).stream()
                .anyMatch(m -> m.getAssociationId().equals(associationId) && unitNumber.equals(m.getUnitNumber()));
        if (!exists) {
            AssociationMembership m = new AssociationMembership();
            m.setUserId(userId);
            m.setAssociationId(associationId);
            m.setRole(role);
            m.setUnitNumber(unitNumber);
            m.setBlockName(blockName);
            m.setActive(true);
            membershipRepository.save(m);
        }
    }

    private void seedEliteTowersTenant(String schema, User treasurer, User owner, User tenant, User president) {
        Integer unitCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".units", Integer.class);
        if (unitCount != null && unitCount > 0) {
            return;
        }
        log.info("Seeding primary tenant schema {} (Elite Towers, Ongole, Andhra Pradesh)...", schema);

        jdbcTemplate.update(
                "INSERT INTO " + schema + ".units (block_name, unit_number, floor_number, area_sqft, bhk_type, owner_user_id, owner_name, owner_phone, occupancy_status, intercom_number, parking_slots) VALUES " +
                "('A', 'A-101', 1, 1600, '3BHK', ?, 'Venkata Subba Rao', '+919848011111', 'OWNER_OCCUPIED', '101', 'P-A101'), " +
                "('A', 'A-102', 1, 1450, '3BHK', ?, 'Lakshmi Narayana', '+919848022222', 'OWNER_OCCUPIED', '102', 'P-A102'), " +
                "('A', 'A-103', 1, 1800, '3BHK', ?, 'Padmavathi Devi', '+919848044444', 'OWNER_OCCUPIED', '103', 'P-A103'), " +
                "('B', 'B-201', 2, 1200, '2BHK', ?, 'Lakshmi Narayana', '+919848022222', 'TENANT_OCCUPIED', '201', 'P-B201')",
                treasurer.getId(), owner.getId(), president.getId(), owner.getId()
        );
        jdbcTemplate.update(
                "UPDATE " + schema + ".units SET tenant_user_id = ?, tenant_name = 'Srinivasulu Reddy', tenant_phone = '+919848033333' WHERE unit_number = 'B-201'",
                tenant.getId()
        );

        Long unitA101 = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".units WHERE unit_number = 'A-101'", Long.class);
        Long unitA102 = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".units WHERE unit_number = 'A-102'", Long.class);
        Long unitB201 = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".units WHERE unit_number = 'B-201'", Long.class);

        // Invoices for Elite Towers (Area-based @ Rs 3.50/sqft + Rs 500 sinking fund + Rs 450 water)
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".invoices (invoice_number, unit_id, billing_month, billing_year, due_date, maintenance_charge, sinking_fund_charge, water_charge, other_charges, late_fee, total_amount, paid_amount, status, notes) VALUES " +
                "('INV-ET-202611-A101', ?, 11, 2026, ?, 5600.00, 500.00, 450.00, 0.00, 0.00, 6550.00, 6550.00, 'PAID', 'Nov 2026 Maintenance - Elite Towers Ongole'), " +
                "('INV-ET-202611-A102', ?, 11, 2026, ?, 5075.00, 500.00, 450.00, 0.00, 0.00, 6025.00, 0.00, 'PENDING', 'Nov 2026 Maintenance - Elite Towers Ongole'), " +
                "('INV-ET-202611-B201', ?, 11, 2026, ?, 4200.00, 500.00, 450.00, 0.00, 0.00, 5150.00, 0.00, 'PENDING', 'Nov 2026 Maintenance - Elite Towers Ongole')",
                unitA101, LocalDate.of(2026, 11, 10),
                unitA102, LocalDate.of(2026, 11, 10),
                unitB201, LocalDate.of(2026, 11, 10)
        );

        Long invA101 = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".invoices WHERE invoice_number = 'INV-ET-202611-A101'", Long.class);
        Long invA102 = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".invoices WHERE invoice_number = 'INV-ET-202611-A102'", Long.class);

        String baseS3 = minioEndpoint + "/" + minioBucket + "/" + schema;
        String a102Proofs = baseS3 + "/receipts/neft-utr-a102.pdf," + baseS3 + "/receipts/bank-advice-a102.png";

        jdbcTemplate.update(
                "INSERT INTO " + schema + ".payments (invoice_id, unit_id, payer_user_id, payer_name, amount, payment_method, reference_number, proof_file_url, proof_file_urls, reconciliation_status, reconciled_by_user_id, reconciled_by_name, audit_notes) VALUES " +
                "(?, ?, ?, 'Venkata Subba Rao', 6550.00, 'RAZORPAY', 'pay_ET998811', ?, ?, 'VERIFIED', ?, 'Venkata Subba Rao', 'Auto-verified via Razorpay Webhook'), " +
                "(?, ?, ?, 'Lakshmi Narayana', 6025.00, 'NEFT', 'SBIN00261101992', ?, ?, 'PENDING_VERIFICATION', NULL, NULL, 'Uploaded NEFT receipt and bank advice from SBI Ongole Main Branch')",
                invA101, unitA101, treasurer.getId(), baseS3 + "/receipts/razorpay-a101.pdf", baseS3 + "/receipts/razorpay-a101.pdf", treasurer.getId(),
                invA102, unitA102, owner.getId(), baseS3 + "/receipts/neft-utr-a102.pdf", a102Proofs
        );

        // Seed Apartment Maintenance Expense Records with multiple MinIO S3 receipt/proof files
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".maintenance_expenses (expense_number, category, title, title_te, vendor_name, amount, expense_date, payment_mode, transaction_reference, receipt_file_urls, recorded_by_user_id, recorded_by_name, approved_by_name, status, notes) VALUES " +
                "('EXP-ET-2026-001', 'ELEVATOR_AMC', 'KONE / Johnson Lifts Quarterly AMC - Block A & B', 'బ్లాక్ A & B లిఫ్ట్ త్రైమాసిక నిర్వహణ (AMC)', 'Sri Sai Elevator Services, Ongole', 28500.00, ?, 'NEFT', 'UTR-SBIN8849201', ?, ?, 'Venkata Subba Rao', 'Padmavathi Devi', 'PAID', 'Includes tax invoice, service report, and bank NEFT confirmation'), " +
                "('EXP-ET-2026-002', 'GENERATOR_DIESEL', '125 kVA Kirloskar DG Set Diesel Refill (200 Litres)', 'జనరేటర్ డీజిల్ కొనుగోలు (200 లీటర్లు)', 'Prakasam Highway HPCL Fuel Station, Ongole', 19400.00, ?, 'UPI', 'UPI-430599182711', ?, ?, 'Venkata Subba Rao', 'Padmavathi Devi', 'PAID', 'Attached fuel pump bill and UPI payment screenshot'), " +
                "('EXP-ET-2026-003', 'WATER_TANKER', 'Supplemental Drinking Water Tankers (6 Loads)', 'తాగునీటి ట్యాంకర్ల సరఫరా (6 ట్యాంకర్లు)', 'Ongole Balaji Water Suppliers', 7200.00, ?, 'CHEQUE', 'CHQ-004819', ?, ?, 'Venkata Subba Rao', 'Padmavathi Devi', 'APPROVED', 'Attached 6 delivery challans and cheque leaf scan')",
                LocalDate.of(2026, 10, 25), baseS3 + "/expenses/lift-amc-invoice.pdf," + baseS3 + "/expenses/lift-service-certificate.pdf," + baseS3 + "/expenses/lift-neft-receipt.png", treasurer.getId(),
                LocalDate.of(2026, 10, 28), baseS3 + "/expenses/hpcl-diesel-bill.jpg," + baseS3 + "/expenses/upi-tx-proof.png", treasurer.getId(),
                LocalDate.of(2026, 10, 30), baseS3 + "/expenses/water-tanker-challans.pdf," + baseS3 + "/expenses/cheque-004819.jpg", treasurer.getId()
        );

        // Helpdesk Tickets
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp plus12h = Timestamp.from(Instant.now().plus(12, ChronoUnit.HOURS));
        Timestamp plus24h = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".helpdesk_tickets (ticket_number, unit_id, unit_number, raised_by_user_id, raised_by_name, category, priority, title, description, attachment_url, status, assigned_technician_name, assigned_technician_phone, sla_due_at, sla_breached) VALUES " +
                "('TKT-ET-1001', ?, 'A-102', ?, 'Lakshmi Narayana', 'PLUMBING', 'HIGH', 'Kitchen inlet valve pressure drop in Flat A-102', 'Low municipal water pressure at kitchenRO inlet line since morning.', ?, 'IN_PROGRESS', 'Koteswara Rao (Plumber)', '+919848055555', ?, false), " +
                "('TKT-ET-1002', ?, 'B-201', ?, 'Srinivasulu Reddy', 'ELECTRICAL', 'MEDIUM', 'Corridor LED tube light flickering on 2nd Floor Block B', 'Common corridor light near lift lobby needs driver replacement.', NULL, 'OPEN', NULL, NULL, ?, false)",
                unitA102, owner.getId(), baseS3 + "/tickets/kitchen-valve-a102.jpg", plus12h,
                unitB201, tenant.getId(), plus24h
        );

        // Amenities
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".amenities (name, name_te, code, description, description_te, capacity, hourly_rate, open_time, close_time, max_hours_per_booking, active) VALUES " +
                "('Elite Clubhouse', 'ఏలైట్ క్లబ్ హౌస్', 'CLUBHOUSE', 'Air-conditioned multipurpose lounge & indoor games', 'ఎయిర్ కండిషన్డ్ మల్టీపర్పస్ హాల్ మరియు ఇండోర్ గేమ్స్', 60, 500.00, '06:00', '22:00', 4, true), " +
                "('Grand Party Hall', 'వేడుకల పార్టీ హాల్', 'PARTY_HALL', 'Banquet hall for birthdays, vrathams & family functions', 'పుట్టినరోజులు, వ్రతాలు మరియు కుటుంబ వేడుకల హాల్', 150, 1200.00, '08:00', '22:00', 6, true), " +
                "('Swimming Pool', 'స్విమ్మింగ్ పూల్', 'SWIMMING_POOL', 'Temperature-controlled lap & kids pool', 'పెద్దలు మరియు పిల్లల స్విమ్మింగ్ పూల్', 30, 0.00, '06:00', '20:00', 2, true), " +
                "('Floodlit Tennis Court', 'టెన్నిస్ కోర్ట్', 'TENNIS_COURT', 'Synthetic outdoor court with LED floodlights', 'LED ఫ్లడ్‌లైట్లతో కూడిన అవుట్‌డోర్ టెన్నిస్ కోర్ట్', 4, 200.00, '06:00', '22:00', 2, true)"
        );

        Long clubhouseId = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".amenities WHERE code = 'CLUBHOUSE'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".amenity_bookings (amenity_id, unit_id, unit_number, booked_by_user_id, booked_by_name, booking_date, start_time, end_time, purpose, guest_count, fee_amount, status) VALUES " +
                "(?, ?, 'A-101', ?, 'Venkata Subba Rao', ?, '18:00', '20:00', 'Karthika Masam Family Gathering', 25, 1000.00, 'CONFIRMED')",
                clubhouseId, unitA101, treasurer.getId(), LocalDate.now()
        );

        // Governance Polls (1 Secret Ballot + 1 Open Poll, both Owner-Only 1-vote-per-unit)
        Timestamp endsAt = Timestamp.from(Instant.now().plus(14, ChronoUnit.DAYS));
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".polls (title, title_te, description, description_te, is_secret, owner_only, status, starts_at, ends_at, created_by_user_id, created_by_name) VALUES " +
                "('Approval for 40kW Rooftop Solar Plant at Elite Towers', 'ఏలైట్ టవర్స్‌లో 40kW రూఫ్‌టాప్ సోలార్ ప్లాంట్ ఏర్పాటు ఆమోదం', 'Resolution to install 40kW grid-tied solar panels under PM Surya Ghar subsidy to reduce common area APSPDCL electricity bills by 65%.', 'ఉమ్మడి కరెంట్ బిల్లులను 65% తగ్గించేందుకు 40kW రూఫ్‌టాప్ సోలార్ ప్లాంట్ ఏర్పాటుకు యజమానుల తీర్మానం.', false, true, 'ACTIVE', ?, ?, ?, 'Padmavathi Devi'), " +
                "('Election of Internal Auditor for FY 2026-27 (Secret Ballot)', '2026-27 ఆర్థిక సంవత్సరానికి అంతర్గత ఆడిటర్ ఎన్నిక (రహస్య ఓటింగ్)', 'Secret ballot strictly for flat owners (1 vote per unit) to appoint the statutory RWA auditor in Ongole.', 'ఒంగోలులో అసోసియేషన్ ఆడిటర్ నియామకం కోసం ఫ్లాట్ యజమానుల రహస్య ఓటింగ్ (ప్రతి ఫ్లాట్‌కు ఒక ఓటు).', true, true, 'ACTIVE', ?, ?, ?, 'Padmavathi Devi')",
                now, endsAt, president.getId(),
                now, endsAt, president.getId()
        );

        Long solarPollId = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".polls WHERE is_secret = false LIMIT 1", Long.class);
        Long secretPollId = jdbcTemplate.queryForObject("SELECT id FROM " + schema + ".polls WHERE is_secret = true LIMIT 1", Long.class);

        jdbcTemplate.update(
                "INSERT INTO " + schema + ".poll_options (poll_id, option_text, option_text_te, display_order) VALUES " +
                "(?, 'Approve Tata Power Solar Proposal (Rs. 18.5L)', 'టాటా పవర్ సోలార్ ప్రతిపాదనకు ఆమోదం (రూ. 18.5 లక్షలు)', 1), " +
                "(?, 'Defer to Next Quarterly GBM', 'తదుపరి సర్వసభ్య సమావేశానికి వాయిదా', 2), " +
                "(?, 'M/s Rao & Associates, Chartered Accountants, Ongole', 'మెసర్స్ రావు & అసోసియేట్స్, చార్టర్డ్ అకౌంటెంట్స్, ఒంగోలు', 1), " +
                "(?, 'M/s Krishna Murthy & Co, Chartered Accountants, Ongole', 'మెసర్స్ కృష్ణమూర్తి & కో, చార్టర్డ్ అకౌంటెంట్స్, ఒంగోలు', 2)",
                solarPollId, solarPollId, secretPollId, secretPollId
        );

        // AGM Document Archive
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".documents (title, title_te, category, financial_year, file_url, file_size_bytes, uploaded_by_user_id, uploaded_by_name) VALUES " +
                "('Elite Towers Annual General Body Meeting (AGM) Minutes', 'ఏలైట్ టవర్స్ వార్షిక సర్వసభ్య సమావేశ (AGM) తీర్మానాలు', 'AGM_MINUTES', '2025-26', ?, 485200, ?, 'Padmavathi Devi'), " +
                "('Audited Income & Expense Statement - Elite Towers Ongole', 'ఆడిట్ చేయబడిన ఆదాయ వ్యయ నివేదిక - ఏలైట్ టవర్స్ ఒంగోలు', 'AUDIT_REPORT', '2024-25', ?, 912400, ?, 'Venkata Subba Rao'), " +
                "('Registered Society Bylaws (AP Apartments Promotion of Construction and Ownership Act)', 'రిజిస్టర్డ్ అసోసియేషన్ బైలాస్ (ఆంధ్రప్రదేశ్ అపార్ట్‌మెంట్ చట్టం)', 'BYLAWS', '2025-26', ?, 1240000, ?, 'Padmavathi Devi')",
                baseS3 + "/documents/elite-towers-agm-minutes-2025-26.pdf", president.getId(),
                baseS3 + "/documents/elite-towers-audit-report-2024-25.pdf", treasurer.getId(),
                baseS3 + "/documents/elite-towers-ap-bylaws.pdf", president.getId()
        );

        // Bilingual Notices
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".notices (title, title_te, content, content_te, category, priority, pinned, published_by_user_id, published_by_name) VALUES " +
                "('Karthika Vanabhojanam & Community Dinner at Elite Towers', 'ఏలైట్ టవర్స్ కార్తీక వనభోజనాలు మరియు ఆత్మీయ సమ్మేళనం', 'All residents of Elite Towers, Ongole are cordially invited to the annual Karthika Masam community dinner on Sunday at 7:00 PM in the Grand Party Hall.', 'ఏలైట్ టవర్స్, ఒంగోలు నివాసితులందరికీ ఆదివారం రాత్రి 7:00 గంటలకు పార్టీ హాల్‌లో జరిగే కార్తీక మాస వనభోజన విందుకు సాదర ఆహ్వానం.', 'EVENT', 'HIGH', true, ?, 'Padmavathi Devi'), " +
                "('Overhead Water Tank Cleaning & Chlorination Schedule', 'ఓవర్ హెడ్ వాటర్ ట్యాంక్ శుభ్రపరచడం మరియు క్లోరినేషన్ ప్రకటన', 'Block A and Block B overhead municipal & borewell water tanks will be cleaned this Saturday between 10:00 AM and 1:00 PM. Kindly store sufficient water.', 'ఈ శనివారం ఉదయం 10:00 నుండి మధ్యాహ్నం 1:00 వరకు బ్లాక్ A మరియు B వాటర్ ట్యాంకులు శుభ్రపరచబడతాయి. దయచేసి తగినంత నీటిని నిల్వ చేసుకోగలరు.', 'MAINTENANCE', 'URGENT', true, ?, 'Venkata Subba Rao')",
                president.getId(), treasurer.getId()
        );
    }

    private void seedSriSaiTenant(String schema, User president) {
        Integer unitCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".units", Integer.class);
        if (unitCount != null && unitCount > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO " + schema + ".units (block_name, unit_number, floor_number, area_sqft, bhk_type, owner_user_id, owner_name, owner_phone, occupancy_status, intercom_number, parking_slots) VALUES " +
                "('C', 'C-101', 1, 1500, '3BHK', ?, 'Padmavathi Devi', '+919848044444', 'OWNER_OCCUPIED', '301', 'P-C101')",
                president.getId()
        );
    }
}

