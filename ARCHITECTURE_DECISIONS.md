# Project Aikya (ఐక్య) - Architecture Decisions Record (ADR)

## ADR-001: Monorepo Layout & Bounded Context Separation
- **Decision:** Structure the repository as a clean two-tier monorepo with a central API contract directory:
  - `contracts/openapi.yaml` — OpenAPI 3.1.0 single source of truth for REST endpoints, DTO schemas, authentication, and RBAC rules.
  - `backend/` — Spring Boot 3.3.5 application (Java 21 compatible bytecode, zero Lombok dependency in favor of Java `record`s and standard POJOs for deterministic compilation across JDK 17/21/26).
  - `frontend/` — Vite + React 18 + TypeScript + Tailwind CSS + Shadcn/Radix UI + TanStack Query + Zustand + `i18next` (`en` and `te` locales).
- **Package Boundaries (Decoupled for Parallel Agent Execution):**
  - `com.aikya.common` — Shared DTOs, enums, role definitions, API wrappers, and exception handling (owned by Lead Orchestrator).
  - `com.aikya.platform` — Hibernate 6 multi-tenancy (`MultiTenantConnectionProvider`, `CurrentTenantIdentifierResolver`), dynamic Flyway schema provisioning (`public` + `aikya_assoc_<id>`), Spring Security 6 stateless JWT filter, Phone + OTP authentication, and association onboarding (owned by Agent 1).
  - `com.aikya.domain` — Core HOA/RWA domain modules: Accounting & Billing (`AREA_BASED` and `FLAT_RATE` + sinking fund + water), Payment gateway mock (Razorpay/Cashfree) + Treasurer offline payment reconciliation, Helpdesk Tickets (`OPEN` -> `IN_PROGRESS` -> `RESOLVED` -> `CLOSED` with SLA), Amenity Bookings (slot-conflict detection), Governance Polls (1-vote-per-unit for `OWNER`) & AGM Document Archive, and Notices (owned by Agent 2).
  - `com.aikya.notification` — Localized notification service rendering English (`en`) and Telugu (`te`) SMS/WhatsApp templates based on `user.preferred_locale`, plus mock S3 storage service (owned by Agent 4).

## ADR-002: Database & Schema-per-Tenant Multi-Tenancy Strategy
- **Database Target:** PostgreSQL 16+ at `192.168.10.80:5432/aikya_db` with user `aikya_app_user` and password `your_secure_password_here` as specified in `application.yml`. For isolated automated test suites when the remote LAN host is unreachable, an embedded PostgreSQL-compatible test profile (`application-test.yml`) is also provided while keeping `application.yml` configured with the exact production database parameters.
- **Public Schema (`public`):**
  - `associations`: Tenant registry mapping association IDs and codes to dedicated PostgreSQL schemas (`aikya_assoc_<id>`), along with billing parameters (`billing_type`: `AREA_BASED`, `FLAT_RATE`, `HYBRID`, `rate_per_sqft`, `flat_maintenance_amount`, `sinking_fund_amount`, `water_charge_per_unit`).
  - `users`: Global identity table indexed by Indian mobile number (`phone_number`), storing `full_name`, `email`, `preferred_locale` (`en` or `te`), and `active` flag.
  - `association_memberships`: Resolves the user-to-association many-to-many access control mapping with `user_id`, `association_id`, `role` (`SUPER_ADMIN`, `PRESIDENT`, `SECRETARY`, `TREASURER`, `COMMITTEE_MEMBER`, `OWNER`, `TENANT`, `SECURITY_STAFF`, `TECHNICIAN`), `unit_number`, `block_name`, and `active` status.
  - `otp_verifications`: Stores active/historical OTP challenges for passwordless authentication.
- **Tenant Schema (`aikya_assoc_<id>`):**
  - Isolated per-association tables: `units`, `invoices`, `payments`, `helpdesk_tickets`, `amenities`, `amenity_bookings`, `polls`, `poll_options`, `poll_votes`, `documents`, `notices`, `notification_logs`.
- **Tenant Context Resolution:**
  - Resolved from the `X-Tenant-ID` HTTP header (e.g., `aikya_assoc_1`) or fallback `tenantSchema` claim in the verified JWT token; defaults to `public` for unauthenticated/public endpoints (`/api/v1/auth/**`, `/api/v1/associations/**`).
  - `MultiTenantConnectionProvider` executes `SET search_path TO <tenant_schema>, public` per connection acquisition and resets to `public` on release, validated against strict regex `^(public|aikya_assoc_[a-zA-Z0-9_]+)$` to prevent SQL injection.

## ADR-003: Role-Based Access Control (ACL) & Governance Rules
- **Roles:**
  - `PRESIDENT`, `SECRETARY`, `TREASURER`, `COMMITTEE_MEMBER` — Managing Committee (MC) roles with administrative privileges.
  - `TREASURER` (and `PRESIDENT`) — Authorized to trigger monthly billing runs and verify/reject offline payment submissions (Cheque/NEFT/UPI reference) with mandatory audit notes.
  - `OWNER` — Eligible to cast votes in Association Governance Polls (`owner_only = true`), strictly enforced at the database and service layer via `UNIQUE (poll_id, unit_id)` so each apartment unit gets at most 1 vote. For secret polls (`is_secret = true`), voter identity is anonymized in tally responses.
  - `TENANT` — Can view unit dues, pay invoices, raise helpdesk tickets, book amenities, and view notices/documents, but cannot vote in `OWNER`-restricted AGM resolutions.

## ADR-004: Localization (`en` & `te`) Strategy
- **Frontend:** `i18next` + `react-i18next` initialized with complete English (`en`) and Telugu (`te`) translation dictionaries (`src/locales/en.json`, `src/locales/te.json`). Uses Google Fonts `Plus Jakarta Sans` (Latin) and `Noto Sans Telugu` / `Mandali` (Telugu script) for crisp Telugu rendering.
- **Backend:** Localized Mustache/text template engine (`NotificationService`) that inspects `user.preferredLocale` (`en` or `te`) and dispatches bilingual SMS/WhatsApp notifications for OTPs, invoice generation, payment reconciliation receipts, and helpdesk status updates.

## ADR-005: Mock External Services
- **SMS / WhatsApp Gateway (`SmsGatewayProvider`):** Defaults to `MockSmsGatewayProvider` which logs formatted SMS/WhatsApp payloads to an in-memory ring buffer and DB `notification_logs` table, plus exposes a dev/test OTP inspection header/response field so automated tests and local users can log in seamlessly without paid SMS credentials.
- **Payment Gateway (`PaymentGatewayProvider`):** Defaults to `MockRazorpayGatewayProvider` generating deterministic order IDs (`order_aikya_<uuid>`) and HMAC-verifiable mock webhooks.
- **Object Storage (`StorageService`):** Defaults to `LocalMockS3StorageService` storing uploaded payment proofs, helpdesk photos, and AGM PDFs locally/in-memory with deterministic URLs.

