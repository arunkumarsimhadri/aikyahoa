# Project Aikya (ఐక్య) — Indian Apartment & Community Management Platform

> **Tagline:** *"Community Living, Unified / మన సంఘం, మన ఐక్యత"*  
> **Primary Association:** **Elite Towers (`ఏలైట్ టవర్స్, ఒంగోలు`)** — Kurnool Road, Ongole, Prakasam District, Andhra Pradesh (`523001`)  
> **Locales:** English (`en`) and Telugu (`te` - తెలుగు) with dynamic runtime switching

---

## Overview

**Aikya (ఐక్య)** is a multi-tenant web application built for Indian Apartment Owner Associations (HOAs / Residential Welfare Associations - RWAs). It implements a **Schema-per-Tenant** PostgreSQL architecture (`public` for global authentication & association directory; `aikya_assoc_<id>` isolated schema per apartment association) and integrates with **MinIO S3** (`http://192.168.10.83/aikya-storage`) for multi-file receipt and transaction proof storage.

---

## Core Capabilities

1. **Hibernate 6+ Schema-per-Tenant Multi-Tenancy & Dynamic Flyway Provisioning**
   - `public` schema stores `associations`, `users`, `association_memberships`, and `otp_verifications`.
   - Each onboarded association dynamically provisions and migrates its own isolated PostgreSQL schema (`aikya_assoc_1` for **Elite Towers, Ongole**; `aikya_assoc_2` for **Sri Sai Aikya Towers RWA**).
2. **Passwordless Phone + OTP Authentication & Stateless JWT**
   - Pluggable `SmsGatewayProvider` with localized English & Telugu OTP messages.
   - Multi-unit & multi-association context switching (`/api/v1/auth/switch-context`).
3. **Accounting, Area-Based Billing & Treasurer Reconciliation**
   - Supports `AREA_BASED` (`sqft × ₹3.50/sqft + Sinking Fund + Water Charges`), `FLAT_RATE`, and `HYBRID` monthly maintenance billing runs.
   - Online payment orders & webhooks (Razorpay / Cashfree mock) and offline Cheque / NEFT / UPI payment submissions with **multiple attached proof files**.
   - 1-click Treasurer reconciliation (`VERIFIED` / `REJECTED`) with mandatory audit notes.
4. **Apartment Maintenance Expense Ledger with Multi-File MinIO S3 Proofs**
   - Record and audit RWA maintenance expenses (`ELEVATOR_AMC`, `GENERATOR_DIESEL`, `COMMON_ELECTRICITY`, `WATER_TANKER`, `SECURITY_CLEANING`, `BOREWELL_PUMP`) with one or more attached vendor tax invoices, vouchers, and bank UTR screenshots stored in **MinIO S3** (`aikya-storage` bucket).
5. **Helpdesk Ticket Lifecycle & SLA Tracking**
   - Full ticket lifecycle (`OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`) with priority-based SLA deadlines (`CRITICAL`: 4h, `HIGH`: 12h, `MEDIUM`: 24h, `LOW`: 72h) and technician assignment.
6. **Amenity Booking Slot-Conflict Resolver**
   - Interactive 2-hour slot booking grid for Clubhouse, Party Hall, Swimming Pool, and Tennis Court with strict server-side overlap prevention (`409 Conflict`).
7. **Governance, Polls (`1-Vote-Per-Unit` for `OWNER`) & AGM Archives**
   - Open and secret-ballot polls enforcing `1-vote-per-unit` (`unique (poll_id, unit_number)`) and `OWNER`-only voting rights, plus financial-year AGM document archives.
8. **Bilingual English (`en`) & Telugu (`te`) UI + Thymeleaf Notification Engine**
   - Full `i18next` / `react-i18next` dictionaries (`en.json` & `te.json`) with `Noto Sans Telugu` typography and Thymeleaf text templates for SMS/WhatsApp dispatches.

---

## Repository Structure

```text
aikya/
├── ARCHITECTURE_DECISIONS.md        # Autonomous architecture & design decisions log
├── contracts/
│   └── openapi.yaml                 # OpenAPI 3.1.0 central API specification
├── k8s/
│   └── aikya-k8s-storage-and-deploy.yaml # Kubernetes ConfigMap, Secret & Deployment manifests
├── backend/                         # Java 17/21 + Spring Boot 3.3 + Hibernate Multi-Tenancy
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/aikya/
│       │   ├── common/              # Shared security, tenant context & ports
│       │   ├── platform/            # Multi-tenancy, Flyway provisioner, JWT & OTP Auth, Seeder
│       │   ├── domain/              # Billing, Payments, Expenses, Helpdesk, Amenities, Governance
│       │   └── notification/        # Thymeleaf localized notifications & MinIO S3 storage service
│       ├── main/resources/
│       │   ├── application.yml      # PostgreSQL (192.168.10.80:5432/aikya_db) & MinIO S3 config
│       │   ├── db/migration/public/ # Public schema Flyway SQL migrations
│       │   └── db/migration/tenant/ # Tenant schema Flyway SQL migrations
│       └── test/java/com/aikya/regression/
│           └── AikyaEndToEndRegressionTest.java # 10-scenario Full-Stack E2E Regression Suite
└── frontend/                        # React 18 + Vite + TypeScript + Tailwind CSS + i18next
    ├── package.json
    ├── src/
    │   ├── locales/                 # Complete English (en.json) & Telugu (te.json) bundles
    │   ├── components/              # Responsive AppShell, Sidebar, TopBar, MobileBottomNav
    │   └── pages/                   # Dashboard, Billing/Expenses, Helpdesk, Amenities, Governance, Notices
    └── e2e/
        └── aikya-e2e-regression.mjs # Frontend & Bilingual Localization Regression Verifier
```

---

## Running Backend & E2E Regression Tests

```bash
cd backend
mvn test
mvn spring-boot:run
```

## Running Frontend

```bash
cd frontend
npm install
npm run dev
```
