-- Tenant Schema Migration (Executed inside each aikya_assoc_<id> schema)

CREATE TABLE IF NOT EXISTS units (
    id BIGSERIAL PRIMARY KEY,
    block_name VARCHAR(32) NOT NULL,
    unit_number VARCHAR(32) NOT NULL UNIQUE,
    floor_number INT NOT NULL DEFAULT 1,
    area_sqft INT NOT NULL,
    bhk_type VARCHAR(16) NOT NULL DEFAULT '3BHK',
    owner_user_id BIGINT,
    owner_name VARCHAR(150),
    owner_phone VARCHAR(20),
    tenant_user_id BIGINT,
    tenant_name VARCHAR(150),
    tenant_phone VARCHAR(20),
    occupancy_status VARCHAR(32) NOT NULL DEFAULT 'OWNER_OCCUPIED',
    intercom_number VARCHAR(16),
    parking_slots VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    billing_month INT NOT NULL,
    billing_year INT NOT NULL,
    due_date DATE NOT NULL,
    maintenance_charge NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    sinking_fund_charge NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    water_charge NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    other_charges NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    late_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_unit_billing_period UNIQUE (unit_id, billing_month, billing_year)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    payer_user_id BIGINT,
    payer_name VARCHAR(150),
    amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    gateway_name VARCHAR(32),
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    reference_number VARCHAR(100),
    proof_file_url TEXT,
    proof_file_urls TEXT,
    reconciliation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    reconciled_by_user_id BIGINT,
    reconciled_by_name VARCHAR(150),
    reconciled_at TIMESTAMP WITH TIME ZONE,
    audit_notes TEXT,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS helpdesk_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(32) NOT NULL UNIQUE,
    unit_id BIGINT REFERENCES units(id) ON DELETE SET NULL,
    unit_number VARCHAR(32),
    raised_by_user_id BIGINT NOT NULL,
    raised_by_name VARCHAR(150),
    category VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    attachment_url TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    assigned_to_user_id BIGINT,
    assigned_technician_name VARCHAR(150),
    assigned_technician_phone VARCHAR(20),
    sla_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS amenities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    name_te VARCHAR(150),
    code VARCHAR(40) NOT NULL UNIQUE,
    description TEXT,
    description_te TEXT,
    capacity INT NOT NULL DEFAULT 25,
    hourly_rate NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    open_time VARCHAR(8) NOT NULL DEFAULT '06:00',
    close_time VARCHAR(8) NOT NULL DEFAULT '22:00',
    max_hours_per_booking INT NOT NULL DEFAULT 4,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS amenity_bookings (
    id BIGSERIAL PRIMARY KEY,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    unit_number VARCHAR(32) NOT NULL,
    booked_by_user_id BIGINT NOT NULL,
    booked_by_name VARCHAR(150),
    booking_date DATE NOT NULL,
    start_time VARCHAR(8) NOT NULL,
    end_time VARCHAR(8) NOT NULL,
    purpose VARCHAR(250),
    guest_count INT NOT NULL DEFAULT 1,
    fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS polls (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    title_te VARCHAR(250),
    description TEXT,
    description_te TEXT,
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    owner_only BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id BIGINT,
    created_by_name VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS poll_options (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_text VARCHAR(200) NOT NULL,
    option_text_te VARCHAR(200),
    display_order INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS poll_votes (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id BIGINT NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    unit_number VARCHAR(32) NOT NULL,
    voter_user_id BIGINT NOT NULL,
    voter_name VARCHAR(150),
    voted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_poll_one_vote_per_unit UNIQUE (poll_id, unit_id)
);

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    title_te VARCHAR(200),
    category VARCHAR(64) NOT NULL,
    financial_year VARCHAR(16) NOT NULL,
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 102400,
    uploaded_by_user_id BIGINT,
    uploaded_by_name VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(250) NOT NULL,
    title_te VARCHAR(250),
    content TEXT NOT NULL,
    content_te TEXT,
    category VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    published_by_user_id BIGINT,
    published_by_name VARCHAR(150),
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT,
    recipient_phone VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(150),
    channel VARCHAR(32) NOT NULL DEFAULT 'SMS',
    template_key VARCHAR(64) NOT NULL,
    locale VARCHAR(8) NOT NULL DEFAULT 'en',
    rendered_message TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maintenance_expenses (
    id BIGSERIAL PRIMARY KEY,
    expense_number VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(64) NOT NULL,
    title VARCHAR(250) NOT NULL,
    title_te VARCHAR(250),
    vendor_name VARCHAR(200) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    expense_date DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_mode VARCHAR(32) NOT NULL DEFAULT 'NEFT',
    transaction_reference VARCHAR(120),
    receipt_file_urls TEXT,
    recorded_by_user_id BIGINT,
    recorded_by_name VARCHAR(150),
    approved_by_name VARCHAR(150),
    status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL DEFAULT 'application/octet-stream',
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    storage_provider VARCHAR(64) NOT NULL DEFAULT 'MINIO_S3',
    storage_path TEXT NOT NULL,
    file_url TEXT NOT NULL,
    uploaded_by_user_id BIGINT,
    uploaded_by_name VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_unit ON invoices(unit_id);
CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_reconcile ON payments(reconciliation_status);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON helpdesk_tickets(status);
CREATE INDEX IF NOT EXISTS idx_bookings_amenity_date ON amenity_bookings(amenity_id, booking_date);

