-- Public Schema Migration: Tenant Registry, Global Users, Association Memberships, and OTP Challenges

CREATE TABLE IF NOT EXISTS associations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    name_te VARCHAR(200),
    code VARCHAR(64) NOT NULL UNIQUE,
    schema_name VARCHAR(64) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Ongole',
    state VARCHAR(100) NOT NULL DEFAULT 'Andhra Pradesh',
    pincode VARCHAR(16) NOT NULL DEFAULT '523001',
    billing_type VARCHAR(32) NOT NULL DEFAULT 'AREA_BASED',
    rate_per_sqft NUMERIC(10, 2) NOT NULL DEFAULT 3.50,
    flat_maintenance_amount NUMERIC(12, 2) NOT NULL DEFAULT 3500.00,
    sinking_fund_amount NUMERIC(12, 2) NOT NULL DEFAULT 500.00,
    water_charge_per_unit NUMERIC(12, 2) NOT NULL DEFAULT 450.00,
    late_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 2.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    full_name_te VARCHAR(150),
    email VARCHAR(150),
    preferred_locale VARCHAR(8) NOT NULL DEFAULT 'en',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS association_memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    association_id BIGINT NOT NULL REFERENCES associations(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    unit_number VARCHAR(32),
    block_name VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_association_unit UNIQUE (user_id, association_id, unit_number)
);

CREATE TABLE IF NOT EXISTS otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_memberships_user ON association_memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_assoc ON association_memberships(association_id);
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_verifications(phone_number);

