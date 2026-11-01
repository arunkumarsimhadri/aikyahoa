#!/usr/bin/env node
/**
 * Project Aikya (ఐక్య) — Indian Apartment & Community Management Platform
 * Standalone Frontend E2E, Localization Parity, Component Wiring & Live API Smoke Verifier
 *
 * Zero external test runner dependencies required — uses Node.js built-in `node:test` and `node:assert/strict`.
 *
 * Usage:
 *   node e2e/aikya-e2e-regression.mjs
 *   LIVE_API_URL=http://localhost:8080 node e2e/aikya-e2e-regression.mjs
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, '..');
const SRC_DIR = path.join(FRONTEND_ROOT, 'src');
const LOCALES_DIR = path.join(SRC_DIR, 'locales');

// Telugu Unicode block range: U+0C00 to U+0C7F
const TELUGU_UNICODE_REGEX = /[\u0C00-\u0C7F]/;

/**
 * Recursively collects all dot-separated leaf and branch key paths from a nested JSON object.
 */
function collectKeyPaths(obj, prefix = '') {
  const keys = [];
  if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
    for (const [k, v] of Object.entries(obj)) {
      const fullKey = prefix ? `${prefix}.${k}` : k;
      keys.push(fullKey);
      if (v && typeof v === 'object' && !Array.isArray(v)) {
        keys.push(...collectKeyPaths(v, fullKey));
      }
    }
  }
  return keys;
}

/**
 * Recursively collects all leaf string values with their dot-separated key paths.
 */
function collectLeafStrings(obj, prefix = '') {
  const leaves = [];
  if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
    for (const [k, v] of Object.entries(obj)) {
      const fullKey = prefix ? `${prefix}.${k}` : k;
      if (typeof v === 'string') {
        leaves.push({ key: fullKey, value: v });
      } else if (v && typeof v === 'object' && !Array.isArray(v)) {
        leaves.push(...collectLeafStrings(v, fullKey));
      }
    }
  }
  return leaves;
}

/**
 * Resolves the first existing file path from a list of candidate relative paths under SRC_DIR.
 */
function resolveExistingSrcFile(candidates) {
  for (const rel of candidates) {
    const abs = path.join(SRC_DIR, rel);
    if (fs.existsSync(abs)) {
      return abs;
    }
  }
  return path.join(SRC_DIR, candidates[0]);
}

// ============================================================================
// Suite 1: Bilingual Localization Parity (English `en.json` & Telugu `te.json`)
// ============================================================================
describe('Aikya E2E Suite 1: Bilingual Localization Parity (en.json & te.json)', () => {
  const enPath = path.join(LOCALES_DIR, 'en.json');
  const tePath = path.join(LOCALES_DIR, 'te.json');

  it('verifies en.json and te.json exist and parse as valid JSON objects', () => {
    assert.ok(fs.existsSync(enPath), `Missing English locale file at ${enPath}`);
    assert.ok(fs.existsSync(tePath), `Missing Telugu locale file at ${tePath}`);

    const enRaw = fs.readFileSync(enPath, 'utf-8');
    const teRaw = fs.readFileSync(tePath, 'utf-8');

    const enJson = JSON.parse(enRaw);
    const teJson = JSON.parse(teRaw);

    assert.equal(typeof enJson, 'object');
    assert.equal(typeof teJson, 'object');
    assert.ok(Object.keys(enJson).length > 0, 'en.json must not be empty');
    assert.ok(Object.keys(teJson).length > 0, 'te.json must not be empty');
  });

  it('verifies 100% recursive key parity between en.json and te.json', () => {
    const enJson = JSON.parse(fs.readFileSync(enPath, 'utf-8'));
    const teJson = JSON.parse(fs.readFileSync(tePath, 'utf-8'));

    const enKeys = collectKeyPaths(enJson).sort();
    const teKeys = collectKeyPaths(teJson).sort();

    const missingInTe = enKeys.filter((k) => !teKeys.includes(k));
    const missingInEn = teKeys.filter((k) => !enKeys.includes(k));

    assert.deepEqual(
      missingInTe,
      [],
      `Keys present in en.json but missing in te.json: ${missingInTe.join(', ')}`
    );
    assert.deepEqual(
      missingInEn,
      [],
      `Keys present in te.json but missing in en.json: ${missingInEn.join(', ')}`
    );
    assert.deepEqual(enKeys, teKeys, 'en.json and te.json key hierarchies must match 100%');
  });

  it('verifies authentic Telugu (తెలుగు) Unicode script and Elite Towers Ongole branding in te.json', () => {
    const enJson = JSON.parse(fs.readFileSync(enPath, 'utf-8'));
    const teJson = JSON.parse(fs.readFileSync(tePath, 'utf-8'));

    // Verify Aikya Telugu script branding
    assert.ok(
      TELUGU_UNICODE_REGEX.test(teJson.appName || ''),
      'te.json appName must contain Telugu script (ఐక్య)'
    );
    assert.ok(
      (teJson.appName || '').includes('ఐక్య'),
      'te.json appName must include "ఐక్య"'
    );

    // Verify Primary Apartment Association: Elite Towers, Ongole (523001)
    assert.ok(
      (enJson.brand?.primaryAssociation || '').includes('Elite Towers'),
      'en.json brand.primaryAssociation must reference Elite Towers'
    );
    assert.ok(
      (enJson.brand?.primaryAssociation || '').includes('Ongole'),
      'en.json brand.primaryAssociation must reference Ongole'
    );
    assert.ok(
      (teJson.brand?.primaryAssociation || '').includes('ఏలైట్ టవర్స్') &&
        (teJson.brand?.primaryAssociation || '').includes('ఒంగోలు'),
      'te.json brand.primaryAssociation must reference ఏలైట్ టవర్స్ and ఒంగోలు'
    );

    // Verify high coverage of Telugu Unicode characters across te.json leaf values
    const teLeaves = collectLeafStrings(teJson);
    assert.ok(teLeaves.length >= 100, `Expected at least 100 localized strings, found ${teLeaves.length}`);

    const teluguScriptLeaves = teLeaves.filter(({ value }) => TELUGU_UNICODE_REGEX.test(value));
    const teluguRatio = teluguScriptLeaves.length / teLeaves.length;

    assert.ok(
      teluguRatio >= 0.85,
      `Expected at least 85% of te.json strings to contain Telugu Unicode characters, got ${(
        teluguRatio * 100
      ).toFixed(1)}% (${teluguScriptLeaves.length}/${teLeaves.length})`
    );
  });

  it('verifies required domain sections including multi-file receipts and maintenance expenses exist in locales', () => {
    const enJson = JSON.parse(fs.readFileSync(enPath, 'utf-8'));
    const teJson = JSON.parse(fs.readFileSync(tePath, 'utf-8'));

    const requiredTopLevelSections = [
      'brand',
      'nav',
      'roles',
      'dashboard',
      'billing',
      'expenses',
      'attachments',
      'reconciliation',
      'helpdesk',
      'amenities',
      'governance',
      'notices',
      'auth',
      'common',
    ];

    for (const section of requiredTopLevelSections) {
      assert.ok(enJson[section], `Missing top-level section "${section}" in en.json`);
      assert.ok(teJson[section], `Missing top-level section "${section}" in te.json`);
    }

    // Verify multi-file upload & expense keys
    assert.ok(enJson.billing.proofFileUrls, 'Missing billing.proofFileUrls in en.json');
    assert.ok(teJson.billing.proofFileUrls, 'Missing billing.proofFileUrls in te.json');
    assert.ok(enJson.expenses.uploadMultipleReceipts, 'Missing expenses.uploadMultipleReceipts in en.json');
    assert.ok(teJson.expenses.uploadMultipleReceipts, 'Missing expenses.uploadMultipleReceipts in te.json');
  });
});

// ============================================================================
// Suite 2: Frontend Pages, Layout Components, i18next & apiClient Wiring
// ============================================================================
describe('Aikya E2E Suite 2: Frontend Pages, Layout Components & Multi-File / Multi-Tenant Wiring', () => {
  const requiredPages = [
    'DashboardPage.tsx',
    'BillingAndReconciliationPage.tsx',
    'HelpdeskPage.tsx',
    'AmenitiesPage.tsx',
    'GovernancePage.tsx',
    'NoticesAndNotificationsPage.tsx',
  ];

  it('verifies all 6 core feature pages exist and wire up i18next (useTranslation) and apiClient', () => {
    for (const pageFile of requiredPages) {
      const pagePath = path.join(SRC_DIR, 'pages', pageFile);
      assert.ok(fs.existsSync(pagePath), `Missing required page file: ${pagePath}`);

      const content = fs.readFileSync(pagePath, 'utf-8');
      assert.ok(
        content.includes('useTranslation') || content.includes('i18n'),
        `${pageFile} must integrate react-i18next (useTranslation)`
      );
      assert.ok(
        content.includes('apiClient'),
        `${pageFile} must wire up apiClient for backend REST communication`
      );
    }
  });

  it('verifies BillingAndReconciliationPage.tsx wires up multi-file receipt/proof uploads and maintenance expenses', () => {
    const billingPagePath = path.join(SRC_DIR, 'pages', 'BillingAndReconciliationPage.tsx');
    const content = fs.readFileSync(billingPagePath, 'utf-8');

    // Verify multi-file upload & expense capabilities in BillingAndReconciliationPage
    assert.ok(
      content.includes('uploadFiles') || content.includes('proofFileUrls') || content.includes('receiptFileUrls'),
      'BillingAndReconciliationPage.tsx must support multi-file receipt/transaction proof uploads'
    );
    assert.ok(
      content.includes('getExpenses') || content.includes('createExpense') || content.includes('expenses'),
      'BillingAndReconciliationPage.tsx must support apartment maintenance expenses ledger'
    );
    assert.ok(
      content.includes('reconcilePayment') && content.includes('submitOfflinePayment'),
      'BillingAndReconciliationPage.tsx must wire up offline payment submission and Treasurer reconciliation'
    );
  });

  it('verifies Layout & Auth components (AppShell, Sidebar, TopBar, MobileBottomNav, LoginModal) exist and wire up i18n/store', () => {
    const layoutComponents = [
      ['components/layout/AppShell.tsx', 'components/AppShell.tsx'],
      ['components/layout/Sidebar.tsx', 'components/Sidebar.tsx'],
      ['components/layout/TopBar.tsx', 'components/TopBar.tsx'],
      ['components/layout/MobileBottomNav.tsx', 'components/MobileBottomNav.tsx'],
      ['components/auth/LoginModal.tsx', 'components/layout/LoginModal.tsx', 'components/LoginModal.tsx'],
    ];

    for (const candidates of layoutComponents) {
      const resolved = resolveExistingSrcFile(candidates);
      assert.ok(
        fs.existsSync(resolved),
        `Missing required layout/auth component (checked ${candidates.join(', ')})`
      );
      const source = fs.readFileSync(resolved, 'utf-8');
      assert.ok(source.length > 100, `Component ${path.basename(resolved)} must not be empty`);
    }
  });

  it('verifies apiClient, useAppStore, and i18n configuration support X-Tenant-ID, Elite Towers Ongole, and multi-file storage', () => {
    const apiClientPath = resolveExistingSrcFile(['lib/apiClient.ts', 'api/apiClient.ts', 'services/apiClient.ts']);
    assert.ok(fs.existsSync(apiClientPath), `Missing apiClient.ts at ${apiClientPath}`);
    const apiClientSrc = fs.readFileSync(apiClientPath, 'utf-8');

    // Verify multi-tenant header & JWT Authorization wiring
    assert.ok(
      apiClientSrc.includes('X-Tenant-ID'),
      'apiClient.ts must inject X-Tenant-ID header for schema-per-tenant isolation'
    );
    assert.ok(
      apiClientSrc.includes('Authorization') && apiClientSrc.includes('Bearer'),
      'apiClient.ts must inject Authorization: Bearer <token> header'
    );

    // Verify Primary Apartment Association: Elite Towers, Ongole (523001)
    assert.ok(
      apiClientSrc.includes('Elite Towers') && apiClientSrc.includes('Ongole') && apiClientSrc.includes('523001'),
      'apiClient.ts must include Elite Towers (Ongole, 523001) association seed/fallback metadata'
    );

    // Verify Multi-File Upload & Expense endpoints
    assert.ok(
      apiClientSrc.includes('/api/v1/storage/upload-multiple'),
      'apiClient.ts must wire up /api/v1/storage/upload-multiple for multi-file receipt/proof uploads'
    );
    assert.ok(
      apiClientSrc.includes('/api/v1/billing/expenses'),
      'apiClient.ts must wire up /api/v1/billing/expenses for society maintenance expenses'
    );

    // Verify Zustand store & i18n bootstrap
    const storePath = resolveExistingSrcFile(['store/useAppStore.ts', 'state/useAppStore.ts']);
    assert.ok(fs.existsSync(storePath), `Missing useAppStore.ts at ${storePath}`);
    const storeSrc = fs.readFileSync(storePath, 'utf-8');
    assert.ok(
      storeSrc.includes('Elite Towers') && storeSrc.includes('aikya_assoc_1'),
      'useAppStore.ts must default to Elite Towers (aikya_assoc_1)'
    );

    const i18nPath = resolveExistingSrcFile(['i18n/index.ts', 'i18n.ts', 'lib/i18n.ts']);
    assert.ok(fs.existsSync(i18nPath), `Missing i18n initialization file at ${i18nPath}`);
  });
});

// ============================================================================
// Suite 3: Live HTTP Smoke & E2E API Assertions (when LIVE_API_URL is set)
// ============================================================================
const LIVE_API_URL = process.env.LIVE_API_URL ? process.env.LIVE_API_URL.replace(/\/+$/, '') : '';

describe('Aikya E2E Suite 3: Live REST API Smoke & Regression Verification', () => {
  if (!LIVE_API_URL) {
    it('skips live HTTP smoke assertions when LIVE_API_URL environment variable is not set', (t) => {
      t.skip('Set LIVE_API_URL=http://localhost:8080 to run live HTTP E2E smoke tests against the Spring Boot backend');
    });
    return;
  }

  let treasurerToken = '';
  let ownerToken = '';
  let tenantToken = '';

  async function apiFetch(endpoint, { method = 'GET', token = '', tenant = 'aikya_assoc_1', body } = {}) {
    const headers = {
      'X-Tenant-ID': tenant,
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    let payloadBody = body;
    if (body && !(body instanceof FormData) && typeof body === 'object') {
      headers['Content-Type'] = 'application/json';
      payloadBody = JSON.stringify(body);
    }
    const res = await fetch(`${LIVE_API_URL}${endpoint}`, {
      method,
      headers,
      body: payloadBody,
    });
    const text = await res.text();
    let json = null;
    try {
      json = text ? JSON.parse(text) : null;
    } catch {
      json = { raw: text };
    }
    return { status: res.status, ok: res.ok, body: json };
  }

  function unwrapData(body) {
    if (body && typeof body === 'object' && 'data' in body && body.data !== undefined) {
      return body.data;
    }
    return body;
  }

  it('verifies Elite Towers (Ongole, AP 523001) association is active in GET /api/v1/associations', async () => {
    const res = await apiFetch('/api/v1/associations');
    assert.equal(res.status, 200);
    const list = unwrapData(res.body);
    assert.ok(Array.isArray(list) && list.length >= 2, 'Expected at least 2 seeded associations');

    const eliteTowers = list.find((a) => a.schemaName === 'aikya_assoc_1');
    assert.ok(eliteTowers, 'Expected aikya_assoc_1 (Elite Towers) in associations list');
    assert.equal(eliteTowers.name, 'Elite Towers');
    assert.equal(eliteTowers.city, 'Ongole');
    assert.equal(eliteTowers.pincode, '523001');
  });

  it('authenticates Treasurer, Owner, and Tenant via Phone + OTP and switches locale to Telugu (te)', async () => {
    const personas = [
      { phone: '+919848011111', role: 'TREASURER' },
      { phone: '+919848022222', role: 'OWNER' },
      { phone: '+919848033333', role: 'TENANT' },
    ];

    for (const p of personas) {
      const sendRes = await apiFetch('/api/v1/auth/otp/send', {
        method: 'POST',
        body: { phoneNumber: p.phone },
      });
      assert.equal(sendRes.status, 200);
      const sendData = unwrapData(sendRes.body);
      const otpCode = sendData.mockOtpCode || '123456';

      const verifyRes = await apiFetch('/api/v1/auth/otp/verify', {
        method: 'POST',
        body: { phoneNumber: p.phone, otpCode },
      });
      assert.equal(verifyRes.status, 200);
      const verifyData = unwrapData(verifyRes.body);
      const token = verifyData.accessToken || verifyData.token;
      assert.ok(token, `Expected JWT token for ${p.role}`);

      if (p.role === 'TREASURER') treasurerToken = token;
      if (p.role === 'OWNER') ownerToken = token;
      if (p.role === 'TENANT') tenantToken = token;
    }

    const localeRes = await apiFetch('/api/v1/users/locale', {
      method: 'PATCH',
      token: ownerToken,
      body: { preferredLocale: 'te' },
    });
    assert.equal(localeRes.status, 200);
    const localeData = unwrapData(localeRes.body);
    assert.equal(localeData.preferredLocale || localeData.locale, 'te');
  });

  it('uploads multiple receipt files and records an Apartment Maintenance Expense with attachments', async () => {
    const formData = new FormData();
    formData.append(
      'files',
      new Blob(['%PDF-1.4 Elite Towers Ongole Invoice 1'], { type: 'application/pdf' }),
      'elite-towers-invoice-1.pdf'
    );
    formData.append(
      'files',
      new Blob(['PNG-DATA Elite Towers SBI NEFT Slip 2'], { type: 'image/png' }),
      'elite-towers-neft-slip-2.png'
    );
    formData.append('category', 'MAINTENANCE_EXPENSE');

    const uploadRes = await apiFetch('/api/v1/storage/upload-multiple', {
      method: 'POST',
      token: treasurerToken,
      body: formData,
    });
    assert.equal(uploadRes.status, 200);
    const uploadedFiles = unwrapData(uploadRes.body);
    assert.ok(Array.isArray(uploadedFiles) && uploadedFiles.length === 2, 'Expected 2 uploaded receipt files');

    const receiptUrls = uploadedFiles.map((f) => f.fileUrl);
    const expenseRes = await apiFetch('/api/v1/billing/expenses', {
      method: 'POST',
      token: treasurerToken,
      body: {
        category: 'GENERATOR_DIESEL',
        title: 'Elite Towers Ongole DG Set Diesel & Servicing',
        titleTe: 'ఏలైట్ టవర్స్ ఒంగోలు జనరేటర్ డీజిల్ & సర్వీసింగ్',
        vendorName: 'Prakasam Power & Fuels, Ongole',
        amount: 9200.0,
        paymentMethod: 'NEFT',
        referenceNumber: 'SBI-OGL-EXP-20261101',
        expenseDate: '2026-11-01',
        receiptFileUrls: receiptUrls,
      },
    });
    assert.ok(expenseRes.status === 200 || expenseRes.status === 201);
    const createdExpense = unwrapData(expenseRes.body);
    assert.ok(
      Array.isArray(createdExpense.receiptFileUrls) && createdExpense.receiptFileUrls.length >= 2,
      'Created expense must persist multi-file receipt URLs'
    );
  });

  it('verifies bilingual Notification Preview renders English and Telugu (ఐక్య) templates', async () => {
    const tePreview = await apiFetch('/api/v1/notifications/preview', {
      method: 'POST',
      token: treasurerToken,
      body: {
        templateKey: 'INVOICE_GENERATED',
        locale: 'te',
        channel: 'WHATSAPP',
        parameters: {
          unitNumber: 'A-102',
          period: '11/2026',
          amount: '5875',
          dueDate: '2026-11-10',
        },
      },
    });
    assert.equal(tePreview.status, 200);
    const previewData = unwrapData(tePreview.body);
    assert.ok(
      TELUGU_UNICODE_REGEX.test(previewData.renderedMessage || ''),
      'Rendered Telugu notification must contain Telugu script'
    );
  });
});
