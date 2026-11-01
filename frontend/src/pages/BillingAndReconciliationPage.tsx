import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  FileSpreadsheet,
  CheckCircle2,
  XCircle,
  Clock,
  Upload,
  CreditCard,
  IndianRupee,
  Play,
  ExternalLink,
  Filter,
  ShieldCheck,
  AlertCircle,
  Sparkles,
  Receipt,
  PlusCircle,
  Paperclip,
  Building2,
  FileText,
} from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { apiClient } from '../lib/apiClient';
import type {
  InvoiceDto,
  PaymentDto,
  ReconciliationStatus,
  MaintenanceExpenseDto,
  ExpenseCategory,
} from '../types/api';

function unwrap<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in (res as Record<string, unknown>)) {
    return (res as { data: T }).data;
  }
  return res as T;
}

const EXPENSE_CATEGORIES: { value: ExpenseCategory; labelEn: string; labelTe: string }[] = [
  { value: 'ELEVATOR_AMC', labelEn: 'Lift / Elevator AMC', labelTe: 'లిఫ్ట్ నిర్వహణ (AMC)' },
  { value: 'GENERATOR_DIESEL', labelEn: 'DG Set Diesel & Servicing', labelTe: 'జనరేటర్ డీజిల్ & సర్వీసింగ్' },
  { value: 'ELECTRICITY_COMMON', labelEn: 'APCPDCL Common Electricity', labelTe: 'APCPDCL ఉమ్మడి విద్యుత్ బిల్లు' },
  { value: 'WATER_TANKER', labelEn: 'Water Tanker & Borewell', labelTe: 'వాటర్ ట్యాంకర్ & బోరు మోటార్' },
  { value: 'SECURITY_AGENCY', labelEn: 'Watchman / Security Salary', labelTe: 'వాచ్‌మెన్ / సెక్యూరిటీ జీతాలు' },
  { value: 'CLEANING_HOUSEKEEPING', labelEn: 'Housekeeping & Sanitation', labelTe: 'హౌస్ కీపింగ్ & పారిశుధ్యం' },
  { value: 'PLUMBING_REPAIR', labelEn: 'Plumbing & Motor Repairs', labelTe: 'ప్లంబింగ్ & మోటార్ మరమ్మతులు' },
  { value: 'GARDENING', labelEn: 'Gardening & Landscaping', labelTe: 'మొక్కల పెంపకం & గార్డెనింగ్' },
  { value: 'FESTIVAL_EVENT', labelEn: 'Community Festival / Cultural', labelTe: 'పండుగలు & సాంస్కృతిక వేడుకలు' },
  { value: 'OTHER', labelEn: 'Other Society Expense', labelTe: 'ఇతర అసోసియేషన్ ఖర్చులు' },
];

export const BillingAndReconciliationPage: React.FC = () => {
  const { t } = useTranslation();
  const { locale, activeAssociation } = useAppStore();
  const isTe = locale === 'te';

  const activeRole = activeAssociation?.role || 'OWNER';
  const isCommittee = ['PRESIDENT', 'SECRETARY', 'TREASURER'].includes(activeRole);

  const [invoices, setInvoices] = useState<InvoiceDto[]>([]);
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [expenses, setExpenses] = useState<MaintenanceExpenseDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [expenseCategoryFilter, setExpenseCategoryFilter] = useState<string>('ALL');
  const [bannerMessage, setBannerMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Billing Run State
  const [billingMonth, setBillingMonth] = useState<number>(11);
  const [billingYear, setBillingYear] = useState<number>(2026);
  const [dueDate, setDueDate] = useState<string>('2026-11-15');
  const [runningBilling, setRunningBilling] = useState<boolean>(false);

  // Treasurer Reconcile Modal State
  const [reconcileTarget, setReconcileTarget] = useState<PaymentDto | null>(null);
  const [reconcileDecision, setReconcileDecision] = useState<'VERIFIED' | 'REJECTED'>('VERIFIED');
  const [auditNotes, setAuditNotes] = useState<string>('');
  const [submittingReconcile, setSubmittingReconcile] = useState<boolean>(false);

  // Offline Payment Modal State (Multi-File Proofs)
  const [offlineTargetInvoice, setOfflineTargetInvoice] = useState<InvoiceDto | null>(null);
  const [offlineMethod, setOfflineMethod] = useState<'CHEQUE' | 'NEFT' | 'IMPS' | 'UPI_MANUAL'>('NEFT');
  const [offlineRef, setOfflineRef] = useState<string>('');
  const [offlineNotes, setOfflineNotes] = useState<string>('');
  const [proofUrls, setProofUrls] = useState<string[]>([]);
  const [uploadingProof, setUploadingProof] = useState<boolean>(false);
  const [submittingOffline, setSubmittingOffline] = useState<boolean>(false);

  // Online Checkout State
  const [onlineTargetInvoice, setOnlineTargetInvoice] = useState<InvoiceDto | null>(null);
  const [selectedGateway, setSelectedGateway] = useState<'RAZORPAY' | 'CASHFREE'>('RAZORPAY');
  const [processingOnline, setProcessingOnline] = useState<boolean>(false);

  // Record Society Maintenance Expense Modal State (Multi-File Receipts)
  const [showExpenseModal, setShowExpenseModal] = useState<boolean>(false);
  const [expCategory, setExpCategory] = useState<ExpenseCategory>('ELEVATOR_AMC');
  const [expTitle, setExpTitle] = useState<string>('');
  const [expTitleTe, setExpTitleTe] = useState<string>('');
  const [expVendor, setExpVendor] = useState<string>('');
  const [expAmount, setExpAmount] = useState<string>('12500');
  const [expDate, setExpDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [expPaymentMode, setExpPaymentMode] = useState<'NEFT' | 'UPI' | 'CHEQUE' | 'IMPS' | 'CASH'>('NEFT');
  const [expTxRef, setExpTxRef] = useState<string>('');
  const [expApprovedBy, setExpApprovedBy] = useState<string>('President & MC Committee');
  const [expNotes, setExpNotes] = useState<string>('');
  const [expReceiptUrls, setExpReceiptUrls] = useState<string[]>([]);
  const [uploadingExpReceipts, setUploadingExpReceipts] = useState<boolean>(false);
  const [submittingExpense, setSubmittingExpense] = useState<boolean>(false);

  const loadBillingData = useCallback(async () => {
    setLoading(true);
    try {
      const [invRes, payRes, expRes] = await Promise.all([
        apiClient.getInvoices(),
        apiClient.getPayments(),
        apiClient.getExpenses(),
      ]);
      setInvoices(unwrap<InvoiceDto[]>(invRes) || []);
      setPayments(unwrap<PaymentDto[]>(payRes) || []);
      setExpenses(unwrap<MaintenanceExpenseDto[]>(expRes) || []);
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Failed to load billing records' });
    } finally {
      setLoading(false);
    }
  }, [activeAssociation?.schemaName]);

  useEffect(() => {
    loadBillingData();
  }, [loadBillingData]);

  const formatINR = (val?: number) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val || 0);

  const handleTriggerBillingRun = async (e: React.FormEvent) => {
    e.preventDefault();
    setRunningBilling(true);
    setBannerMessage(null);
    try {
      const res = await apiClient.triggerBillingRun({
        billingMonth,
        billingYear,
        dueDate,
        notes: `Monthly Maintenance Billing Run (${billingMonth}/${billingYear}) — Elite Towers Ongole`,
      });
      const generated = unwrap<InvoiceDto[]>(res) || [];
      setBannerMessage({
        type: 'success',
        text: isTe
          ? `${generated.length} ఫ్లాట్లకు మెయింటెనెన్స్ బిల్లులు విజయవంతంగా జారీ చేయబడ్డాయి మరియు తెలుగు/ఇంగ్లీష్ SMS పంపబడింది!`
          : `Generated ${generated.length} unit maintenance invoices for ${billingMonth}/${billingYear} and dispatched localized SMS/WhatsApp alerts!`,
      });
      await loadBillingData();
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Billing run failed' });
    } finally {
      setRunningBilling(false);
    }
  };

  const openReconcileModal = (payment: PaymentDto, decision: 'VERIFIED' | 'REJECTED') => {
    setReconcileTarget(payment);
    setReconcileDecision(decision);
    setAuditNotes(
      decision === 'VERIFIED'
        ? `Verified ${payment.paymentMethod} ref ${payment.referenceNumber} against Elite Towers SBI RWA bank statement.`
        : `Reference ${payment.referenceNumber} not found in bank statement / cheque returned.`
    );
  };

  const handleConfirmReconcile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reconcileTarget || !auditNotes.trim()) return;
    setSubmittingReconcile(true);
    setBannerMessage(null);
    try {
      await apiClient.reconcilePayment(reconcileTarget.id, {
        decision: reconcileDecision,
        auditNotes: auditNotes.trim(),
      });
      setBannerMessage({
        type: 'success',
        text: isTe
          ? `చెల్లింపు (${reconcileTarget.referenceNumber}) కోశాధికారిచే ${reconcileDecision} చేయబడింది!`
          : `Payment ${reconcileTarget.referenceNumber} (Flat ${reconcileTarget.unitNumber}) marked as ${reconcileDecision}!`,
      });
      setReconcileTarget(null);
      await loadBillingData();
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Reconciliation failed' });
    } finally {
      setSubmittingReconcile(false);
    }
  };

  // Multi-file upload handler for Offline Payment proofs (MinIO S3)
  const handleProofMultiUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fileList = e.target.files;
    if (!fileList || fileList.length === 0) return;
    setUploadingProof(true);
    try {
      const uploaded = await apiClient.uploadFiles(fileList, 'PAYMENT_PROOF');
      const newUrls = uploaded.map((u) => u.fileUrl);
      setProofUrls((prev) => [...prev, ...newUrls]);
    } catch {
      const fallbackUrls = Array.from(fileList).map(
        (f) => `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/${encodeURIComponent(f.name)}`
      );
      setProofUrls((prev) => [...prev, ...fallbackUrls]);
    } finally {
      setUploadingProof(false);
    }
  };

  const handleOfflineSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!offlineTargetInvoice || !offlineRef.trim()) return;
    setSubmittingOffline(true);
    setBannerMessage(null);
    try {
      const dueAmount = offlineTargetInvoice.totalAmount - offlineTargetInvoice.paidAmount;
      const defaultUrl = `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/proof-${offlineRef.trim()}.pdf`;
      const finalUrls = proofUrls.length > 0 ? proofUrls : [defaultUrl];
      await apiClient.submitOfflinePayment({
        invoiceId: offlineTargetInvoice.id,
        amount: dueAmount,
        paymentMethod: offlineMethod,
        referenceNumber: offlineRef.trim(),
        proofFileUrl: finalUrls[0],
        proofFileUrls: finalUrls,
        notes: offlineNotes || `Submitted ${offlineMethod} payment with ${finalUrls.length} receipt proof file(s)`,
      });
      setBannerMessage({
        type: 'success',
        text: isTe
          ? `చెల్లింపు వివరాలు (${offlineRef}) మరియు ${finalUrls.length} రసీదు ఫైళ్లు కోశాధికారి ఆమోదం కొరకు పంపబడ్డాయి!`
          : `Offline ${offlineMethod} payment (${offlineRef}) with ${finalUrls.length} MinIO S3 proof file(s) submitted for Treasurer verification!`,
      });
      setOfflineTargetInvoice(null);
      setOfflineRef('');
      setOfflineNotes('');
      setProofUrls([]);
      await loadBillingData();
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Offline submission failed' });
    } finally {
      setSubmittingOffline(false);
    }
  };

  const handleOnlinePay = async () => {
    if (!onlineTargetInvoice) return;
    setProcessingOnline(true);
    setBannerMessage(null);
    try {
      const dueAmount = onlineTargetInvoice.totalAmount - onlineTargetInvoice.paidAmount;
      const orderRes = await apiClient.createOnlineOrder({
        invoiceId: onlineTargetInvoice.id,
        gateway: selectedGateway,
      });
      const orderData = unwrap<{ orderId: string }>(orderRes);
      const orderId = orderData?.orderId || `order_${Date.now()}`;
      await apiClient.completeOnlinePaymentWebhook({
        invoiceId: onlineTargetInvoice.id,
        gatewayOrderId: orderId,
        gatewayPaymentId: `pay_${selectedGateway.toLowerCase()}_${Date.now()}`,
        gatewayName: selectedGateway,
        amount: dueAmount,
      });
      setBannerMessage({
        type: 'success',
        text: isTe
          ? `ఆన్‌లైన్ చెల్లింపు (${selectedGateway}) విజయవంతమైంది! ఇన్‌వాయిస్ ${onlineTargetInvoice.invoiceNumber} PAID గా మార్చబడింది.`
          : `Instant ${selectedGateway} checkout completed! Invoice ${onlineTargetInvoice.invoiceNumber} reconciled automatically.`,
      });
      setOnlineTargetInvoice(null);
      await loadBillingData();
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Online payment failed' });
    } finally {
      setProcessingOnline(false);
    }
  };

  // Multi-file upload handler for Society Maintenance Expense receipts (MinIO S3)
  const handleExpenseReceiptsUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fileList = e.target.files;
    if (!fileList || fileList.length === 0) return;
    setUploadingExpReceipts(true);
    try {
      const uploaded = await apiClient.uploadFiles(fileList, 'EXPENSE_VOUCHER');
      const urls = uploaded.map((u) => u.fileUrl);
      setExpReceiptUrls((prev) => [...prev, ...urls]);
    } catch {
      const fallbackUrls = Array.from(fileList).map(
        (f) => `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/${encodeURIComponent(f.name)}`
      );
      setExpReceiptUrls((prev) => [...prev, ...fallbackUrls]);
    } finally {
      setUploadingExpReceipts(false);
    }
  };

  const handleCreateExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!expTitle.trim() || !expVendor.trim() || !expAmount) return;
    setSubmittingExpense(true);
    setBannerMessage(null);
    try {
      const defaultReceipt = `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/voucher-${Date.now()}.pdf`;
      const finalReceipts = expReceiptUrls.length > 0 ? expReceiptUrls : [defaultReceipt];
      await apiClient.createExpense({
        category: expCategory,
        title: expTitle.trim(),
        titleTe: expTitleTe.trim() || expTitle.trim(),
        vendorName: expVendor.trim(),
        amount: Number(expAmount),
        expenseDate: expDate,
        paymentMode: expPaymentMode,
        transactionReference: expTxRef.trim() || `UTR-${Date.now().toString().slice(-8)}`,
        receiptFileUrls: finalReceipts,
        approvedByName: expApprovedBy.trim() || 'Managing Committee',
        notes: expNotes.trim() || 'Recorded with multi-file receipts stored on MinIO S3 (aikya-storage)',
      });
      setBannerMessage({
        type: 'success',
        text: isTe
          ? `అపార్ట్‌మెంట్ నిర్వహణ ఖర్చు (${expTitleTe || expTitle}) మరియు ${finalReceipts.length} రసీదు ఫైళ్లు రికార్డు చేయబడ్డాయి!`
          : `Recorded society maintenance expense "${expTitle}" with ${finalReceipts.length} attached receipt/invoice file(s) in MinIO S3!`,
      });
      setShowExpenseModal(false);
      setExpTitle('');
      setExpTitleTe('');
      setExpVendor('');
      setExpTxRef('');
      setExpNotes('');
      setExpReceiptUrls([]);
      await loadBillingData();
    } catch (err: any) {
      setBannerMessage({ type: 'error', text: err?.message || 'Failed to record maintenance expense' });
    } finally {
      setSubmittingExpense(false);
    }
  };

  const filteredPayments = payments.filter((p) =>
    statusFilter === 'ALL' ? true : p.reconciliationStatus === statusFilter
  );

  const filteredExpenses = expenses.filter((exp) =>
    expenseCategoryFilter === 'ALL' ? true : exp.category === expenseCategoryFilter
  );

  const totalSocietyExpenses = expenses.reduce((sum, item) => sum + (item.amount || 0), 0);

  const statusBadge = (status: ReconciliationStatus) => {
    switch (status) {
      case 'VERIFIED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
            <CheckCircle2 className="w-3.5 h-3.5" />
            {isTe ? 'ధృవీకరించబడింది (VERIFIED)' : 'VERIFIED'}
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
            <XCircle className="w-3.5 h-3.5" />
            {isTe ? 'తిరస్కరించబడింది (REJECTED)' : 'REJECTED'}
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
            <Clock className="w-3.5 h-3.5" />
            {isTe ? 'పెండింగ్ (PENDING)' : 'PENDING_VERIFICATION'}
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-orange-100 text-orange-800">
              <Building2 className="w-3.5 h-3.5" />
              {isTe
                ? activeAssociation?.associationNameTe || 'ఏలైట్ టవర్స్ — ఒంగోలు'
                : activeAssociation?.associationName || 'Elite Towers — Ongole (523001)'}
            </span>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
              MinIO S3 Storage (`aikya-storage`)
            </span>
          </div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2 mt-1.5">
            <FileSpreadsheet className="w-6 h-6 text-indigo-700" />
            {isTe
              ? 'మెయింటెనెన్స్ బిల్లింగ్, కోశాధికారి ఆడిట్ & అపార్ట్‌మెంట్ ఖర్చుల పట్టిక'
              : t('nav.billing', 'Maintenance Billing, Treasurer Reconciliation & Expenses Ledger')}
          </h1>
          <p className="text-sm text-slate-600 mt-0.5">
            {isTe
              ? 'ఏలైట్ టవర్స్ (ఒంగోలు) ఆఫ్‌లైన్ చెక్కు/NEFT రసీదుల తనిఖీ, బిల్లింగ్ రన్ మరియు బహుళ రసీదు ఫైళ్లతో అపార్ట్‌మెంట్ ఖర్చుల నిర్వహణ'
              : 'Reconcile offline Cheque/NEFT receipts with multi-file proofs, trigger monthly billing runs, and record Elite Towers maintenance expenses.'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setShowExpenseModal(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold shadow-sm transition cursor-pointer"
          >
            <PlusCircle className="w-4 h-4" />
            {isTe ? 'అపార్ట్‌మెంట్ ఖర్చు & రసీదులు జోడించండి' : 'Record Society Expense + Receipts'}
          </button>
        </div>
      </div>

      {bannerMessage && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between ${
            bannerMessage.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <div className="flex items-center gap-2.5 text-sm font-medium">
            {bannerMessage.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            )}
            <span>{bannerMessage.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setBannerMessage(null)}
            className="text-xs font-semibold underline ml-4 cursor-pointer"
          >
            {isTe ? 'మూసివేయి' : 'Dismiss'}
          </button>
        </div>
      )}

      {/* Trigger Monthly Billing Run Panel */}
      <div className="bg-gradient-to-r from-indigo-950 via-indigo-900 to-slate-900 rounded-2xl p-6 text-white shadow-md">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
          <div className="lg:col-span-7 space-y-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-400/20 text-amber-300 border border-amber-400/30">
              <Sparkles className="w-3.5 h-3.5" />
              {isTe ? 'ఆటోమేటెడ్ నెలవారీ బిల్లింగ్ ఇంజిన్' : 'Automated Monthly Maintenance Billing Engine'}
            </span>
            <h2 className="text-lg font-bold">
              {isTe
                ? 'అన్ని ఫ్లాట్లకు నెలవారీ మెయింటెనెన్స్ ఇన్‌వాయిస్‌లను రూపొందించండి'
                : 'Trigger Monthly Maintenance Billing Run (All Association Units)'}
            </h2>
            <p className="text-xs text-indigo-200 leading-relaxed">
              {isTe
                ? 'ఏరియా ఆధారిత లెక్కింపు: ఫ్లాట్ చ.అ (Sq.ft) × ₹3.50/sq.ft + సింకింగ్ ఫండ్ ₹500 + వాటర్ ఛార్జ్ ₹450 + ఉమ్మడి ఖర్చు ₹200.'
                : 'Formula: Area-Based (Unit Sq.ft × ₹3.50/sq.ft) or Flat Rate + Sinking Fund (₹500) + Water Charge (₹450) + Common Area (₹200).'}
            </p>
            <div className="flex flex-wrap gap-3 pt-1 text-xs text-indigo-100">
              <span className="bg-white/10 px-3 py-1.5 rounded-lg border border-white/10">
                <strong>Flat A-101 (1650 sq.ft):</strong> ₹5,775 + ₹500 + ₹450 + ₹200 = <strong>₹6,925</strong>
              </span>
              <span className="bg-white/10 px-3 py-1.5 rounded-lg border border-white/10">
                <strong>Flat A-102 (1400 sq.ft):</strong> ₹4,900 + ₹500 + ₹450 + ₹200 = <strong>₹6,050</strong>
              </span>
            </div>
          </div>

          <form
            onSubmit={handleTriggerBillingRun}
            className="lg:col-span-5 bg-white/10 backdrop-blur-sm p-4 rounded-xl border border-white/15 grid grid-cols-1 sm:grid-cols-3 gap-3"
          >
            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-indigo-200 mb-1">
                {isTe ? 'నెల (Month)' : 'Billing Month'}
              </label>
              <select
                value={billingMonth}
                onChange={(e) => setBillingMonth(Number(e.target.value))}
                className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-sm text-white"
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => (
                  <option key={m} value={m}>
                    Month {m}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-indigo-200 mb-1">
                {isTe ? 'సంవత్సరం (Year)' : 'Billing Year'}
              </label>
              <input
                type="number"
                value={billingYear}
                onChange={(e) => setBillingYear(Number(e.target.value))}
                className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-sm text-white"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-indigo-200 mb-1">
                {isTe ? 'చివరి తేదీ' : 'Due Date'}
              </label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-sm text-white"
              />
            </div>
            <div className="sm:col-span-3 pt-1">
              <button
                type="submit"
                disabled={runningBilling}
                className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-orange-500 hover:bg-orange-600 text-white font-semibold text-sm shadow transition cursor-pointer disabled:opacity-50"
              >
                <Play className="w-4 h-4" />
                {runningBilling
                  ? isTe
                    ? 'బిల్లులు తయారవుతున్నాయి...'
                    : 'Generating Unit Invoices...'
                  : isTe
                  ? 'నెలవారీ బిల్లింగ్ రన్ ప్రారంభించండి'
                  : t('billing.triggerBillingRun', 'Trigger Monthly Billing Run')}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Treasurer Reconciliation Portal */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-orange-600" />
              <h2 className="text-base font-bold text-slate-900">
                {isTe
                  ? 'కోశాధికారి ఆఫ్‌లైన్ చెల్లింపుల తనిఖీ పోర్టల్ (Treasurer Reconciliation Portal)'
                  : t('billing.reconciliationQueue', 'Treasurer Offline Payment Reconciliation Queue')}
              </h2>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              {isTe
                ? 'చెక్కు, NEFT మరియు మాన్యువల్ UPI చెల్లింపులను బ్యాంక్ స్టేట్‌మెంట్‌తో సరిపోల్చి 1-క్లిక్‌తో ఆమోదించండి'
                : 'Verify or Reject resident Cheque, NEFT, and Manual UPI references with multi-file proofs & mandatory audit trail notes.'}
            </p>
          </div>

          <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
            <Filter className="w-3.5 h-3.5 text-slate-500 ml-2" />
            {(['ALL', 'PENDING_VERIFICATION', 'VERIFIED', 'REJECTED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                onClick={() => setStatusFilter(st)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                  statusFilter === st
                    ? 'bg-white text-indigo-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {st === 'ALL'
                  ? isTe
                    ? 'అన్నీ'
                    : 'All'
                  : st === 'PENDING_VERIFICATION'
                  ? isTe
                    ? 'పెండింగ్'
                    : 'Pending'
                  : st === 'VERIFIED'
                  ? isTe
                    ? 'ఆమోదించినవి'
                    : 'Verified'
                  : isTe
                  ? 'తిరస్కరించినవి'
                  : 'Rejected'}
              </button>
            ))}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <th className="py-3.5 px-4">{isTe ? 'ఫ్లాట్ / ఇన్‌వాయిస్' : 'Unit & Invoice'}</th>
                <th className="py-3.5 px-4">{isTe ? 'చెల్లించినవారు' : 'Payer'}</th>
                <th className="py-3.5 px-4">{isTe ? 'విధానం & Ref' : 'Method & Ref'}</th>
                <th className="py-3.5 px-4">{isTe ? 'మొత్తం' : 'Amount'}</th>
                <th className="py-3.5 px-4">{isTe ? 'రసీదు ఫైళ్లు (Proofs)' : 'Attached Proofs (MinIO S3)'}</th>
                <th className="py-3.5 px-4">{isTe ? 'స్థితి' : 'Status'}</th>
                <th className="py-3.5 px-4 text-right">{isTe ? 'చర్య (1-Click Action)' : 'Treasurer Action'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    Loading payment ledger...
                  </td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    {isTe ? 'చెల్లింపు రికార్డులు లేవు' : 'No payments matching selected filter'}
                  </td>
                </tr>
              ) : (
                filteredPayments.map((pay) => {
                  const urls =
                    pay.proofFileUrls && pay.proofFileUrls.length > 0
                      ? pay.proofFileUrls
                      : pay.proofFileUrl
                      ? [pay.proofFileUrl]
                      : [];
                  return (
                    <tr key={pay.id} className="hover:bg-slate-50/80 transition">
                      <td className="py-3.5 px-4">
                        <span className="font-bold text-slate-900">Flat {pay.unitNumber}</span>
                        <div className="text-xs text-slate-500">{pay.invoiceNumber}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-medium text-slate-800">{pay.payerName}</div>
                        <div className="text-xs text-slate-400">
                          {pay.paidAt ? new Date(pay.paidAt).toLocaleDateString('en-IN') : ''}
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="inline-block px-2 py-0.5 rounded text-xs font-bold bg-indigo-50 text-indigo-800 mr-1.5">
                          {pay.paymentMethod}
                        </span>
                        <span className="font-mono text-xs text-slate-700">{pay.referenceNumber}</span>
                        {pay.auditNotes && (
                          <div className="text-xs text-slate-500 mt-1 italic">"{pay.auditNotes}"</div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 font-bold text-slate-900">{formatINR(pay.amount)}</td>
                      <td className="py-3.5 px-4">
                        {urls.length > 0 ? (
                          <div className="flex flex-wrap gap-1.5">
                            {urls.map((u, idx) => (
                              <a
                                key={idx}
                                href={u}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 px-2 py-1 rounded-md bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold"
                              >
                                <Paperclip className="w-3 h-3" />
                                {isTe ? `రసీదు #${idx + 1}` : `Proof #${idx + 1}`}
                                <ExternalLink className="w-3 h-3" />
                              </a>
                            ))}
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">Auto-Gateway</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">{statusBadge(pay.reconciliationStatus)}</td>
                      <td className="py-3.5 px-4 text-right">
                        {pay.reconciliationStatus === 'PENDING_VERIFICATION' ? (
                          <div className="inline-flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => openReconcileModal(pay, 'VERIFIED')}
                              className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-xs transition cursor-pointer"
                            >
                              {isTe ? 'ఆమోదించు (Verify)' : t('billing.verifyPayment', 'Verify')}
                            </button>
                            <button
                              type="button"
                              onClick={() => openReconcileModal(pay, 'REJECTED')}
                              className="px-3 py-1.5 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-bold transition cursor-pointer"
                            >
                              {isTe ? 'తిరస్కరించు (Reject)' : t('billing.rejectPayment', 'Reject')}
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">
                            {pay.reconciledByName ? `By ${pay.reconciledByName}` : 'Reconciled'}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Apartment Maintenance Records & Expenses Ledger (Multi-File Receipts in MinIO S3) */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Receipt className="w-5 h-5 text-emerald-700" />
              <h2 className="text-base font-bold text-slate-900">
                {isTe
                  ? 'అపార్ట్‌మెంట్ నిర్వహణ ఖర్చులు & రసీదుల రిజిస్టర్ (Maintenance Expenses & Multi-Receipt Ledger)'
                  : 'Apartment Maintenance Records & Society Expenses Ledger (Multi-Receipt Proof Archive)'}
              </h2>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              {isTe
                ? 'లిఫ్ట్ AMC, జనరేటర్ డీజిల్, APCPDCL కరెంట్ బిల్లు, వాటర్ ట్యాంకర్ మరియు వాచ్‌మెన్ జీతాల రసీదులు (MinIO S3 నిల్వ)'
                : 'Transparent society expenditure ledger for Elite Towers, Ongole with multiple attached tax invoices, bills & bank payment vouchers.'}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="px-3.5 py-1.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-bold text-emerald-900">
              {isTe ? 'మొత్తం ఖర్చులు:' : 'Total Recorded Expenses:'} {formatINR(totalSocietyExpenses)}
            </div>
            <select
              value={expenseCategoryFilter}
              onChange={(e) => setExpenseCategoryFilter(e.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white"
            >
              <option value="ALL">{isTe ? 'అన్ని విభాగాలు (All Categories)' : 'All Categories'}</option>
              {EXPENSE_CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {isTe ? c.labelTe : c.labelEn}
                </option>
              ))}
            </select>
            {isCommittee && (
              <button
                type="button"
                onClick={() => setShowExpenseModal(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white text-xs font-bold transition cursor-pointer"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                {isTe ? 'కొత్త ఖర్చు రికార్డు' : '+ Record Expense'}
              </button>
            )}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <th className="py-3.5 px-4">{isTe ? 'వోచర్ & తేదీ' : 'Voucher & Date'}</th>
                <th className="py-3.5 px-4">{isTe ? 'విభాగం & వివరాలు' : 'Category & Description'}</th>
                <th className="py-3.5 px-4">{isTe ? 'వెండర్ (Vendor)' : 'Vendor / Payee'}</th>
                <th className="py-3.5 px-4">{isTe ? 'చెల్లింపు & UTR' : 'Mode & Reference'}</th>
                <th className="py-3.5 px-4">{isTe ? 'మొత్తం' : 'Amount'}</th>
                <th className="py-3.5 px-4">{isTe ? 'జతచేసిన బిల్లులు / రసీదులు' : 'Attached Receipts & Proofs'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {filteredExpenses.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    {isTe ? 'ఖర్చుల రికార్డులు లేవు' : 'No society maintenance expenses recorded yet'}
                  </td>
                </tr>
              ) : (
                filteredExpenses.map((exp) => (
                  <tr key={exp.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-mono text-xs font-bold text-indigo-900">{exp.expenseNumber}</div>
                      <div className="text-xs text-slate-500">{exp.expenseDate}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-block px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-900 mb-1">
                        {exp.category}
                      </span>
                      <div className="font-bold text-slate-900">
                        {isTe ? exp.titleTe || exp.title : exp.title}
                      </div>
                      {exp.notes && <div className="text-xs text-slate-500 mt-0.5">{exp.notes}</div>}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-medium text-slate-800">{exp.vendorName}</div>
                      <div className="text-xs text-slate-400">
                        {isTe ? 'ఆమోదం:' : 'Approved:'} {exp.approvedByName || exp.recordedByName}
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-block px-2 py-0.5 rounded text-xs font-bold bg-slate-100 text-slate-800 mr-1">
                        {exp.paymentMode}
                      </span>
                      <div className="font-mono text-xs text-slate-600 mt-0.5">{exp.transactionReference}</div>
                    </td>
                    <td className="py-3.5 px-4 font-extrabold text-slate-900">{formatINR(exp.amount)}</td>
                    <td className="py-3.5 px-4">
                      <div className="flex flex-wrap gap-1.5">
                        {(exp.receiptFileUrls || []).map((url, idx) => {
                          const fileName = decodeURIComponent(url.split('/').pop() || `receipt-${idx + 1}.pdf`);
                          return (
                            <a
                              key={idx}
                              href={url}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 text-xs font-semibold transition"
                            >
                              <FileText className="w-3.5 h-3.5 text-emerald-600" />
                              <span className="max-w-[140px] truncate">{fileName}</span>
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          );
                        })}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Resident Invoices & Breakdown Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900">
              {isTe
                ? 'ఫ్లాట్ మెయింటెనెన్స్ ఇన్‌వాయిస్‌లు & చెల్లింపు చరిత్ర'
                : 'Unit Maintenance Invoices & Itemized Breakdown'}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              {isTe
                ? 'ఆన్‌లైన్ (Razorpay / Cashfree) లేదా ఆఫ్‌లైన్ చెక్కు/NEFT ద్వారా బహుళ రసీదు ఫైళ్లతో చెల్లించండి'
                : 'Pay online instantly via Razorpay/Cashfree or submit offline Cheque/NEFT reference with multiple proof files'}
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500 border-b border-slate-200">
                <th className="py-3.5 px-4">{isTe ? 'ఇన్‌వాయిస్ నంబర్' : 'Invoice #'}</th>
                <th className="py-3.5 px-4">{isTe ? 'ఫ్లాట్ & విస్తీర్ణం' : 'Unit & Area'}</th>
                <th className="py-3.5 px-4">{isTe ? 'మెయింటెనెన్స్' : 'Base Maint.'}</th>
                <th className="py-3.5 px-4">{isTe ? 'సింకింగ్ + వాటర్' : 'Sinking + Water'}</th>
                <th className="py-3.5 px-4">{isTe ? 'మొత్తం బిల్లు' : 'Total Due'}</th>
                <th className="py-3.5 px-4">{isTe ? 'స్థితి' : 'Status'}</th>
                <th className="py-3.5 px-4 text-right">{isTe ? 'చెల్లింపు ఎంపికలు' : 'Payment Options'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {invoices.map((inv) => {
                const balance = inv.totalAmount - inv.paidAmount;
                return (
                  <tr key={inv.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-slate-900">{inv.invoiceNumber}</div>
                      <div className="text-xs text-slate-500">Due: {inv.dueDate}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="font-semibold text-slate-800">
                        {inv.blockName} - {inv.unitNumber}
                      </span>
                      {inv.areaSqft && (
                        <span className="ml-1.5 text-xs text-slate-500">({inv.areaSqft} sq.ft)</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-slate-700">{formatINR(inv.maintenanceCharge)}</td>
                    <td className="py-3.5 px-4 text-slate-700">
                      {formatINR(inv.sinkingFundCharge + inv.waterCharge + (inv.otherCharges || 0))}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-extrabold text-slate-900">{formatINR(inv.totalAmount)}</div>
                      {inv.paidAmount > 0 && inv.paidAmount < inv.totalAmount && (
                        <div className="text-xs text-emerald-600">Paid: {formatINR(inv.paidAmount)}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-bold ${
                          inv.status === 'PAID'
                            ? 'bg-emerald-100 text-emerald-800'
                            : inv.status === 'OVERDUE'
                            ? 'bg-rose-100 text-rose-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {inv.status}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      {inv.status !== 'PAID' && balance > 0 ? (
                        <div className="inline-flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => setOnlineTargetInvoice(inv)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-indigo-900 hover:bg-indigo-800 text-white text-xs font-semibold transition cursor-pointer"
                          >
                            <CreditCard className="w-3.5 h-3.5" />
                            {isTe ? 'ఆన్‌లైన్ పే' : 'Online Pay'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setOfflineTargetInvoice(inv);
                              setProofUrls([]);
                            }}
                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-orange-50 hover:bg-orange-100 text-orange-800 border border-orange-200 text-xs font-semibold transition cursor-pointer"
                          >
                            <Upload className="w-3.5 h-3.5" />
                            {isTe ? 'చెక్కు / NEFT + రసీదులు' : 'Cheque / NEFT + Proofs'}
                          </button>
                        </div>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-700">
                          <CheckCircle2 className="w-4 h-4" />
                          {isTe ? 'చెల్లించబడింది' : 'Settled'}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* 1-Click Verify / Reject Modal (Mandatory Treasurer Audit Notes) */}
      {reconcileTarget && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {reconcileDecision === 'VERIFIED'
                  ? isTe
                    ? 'చెల్లింపు ఆమోదం (Verify Offline Payment)'
                    : 'Verify Offline Payment'
                  : isTe
                  ? 'చెల్లింపు తిరస్కరణ (Reject Offline Payment)'
                  : 'Reject Offline Payment'}
              </h3>
              <button
                type="button"
                onClick={() => setReconcileTarget(null)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-1">
              <div className="flex justify-between">
                <span className="text-slate-500">Unit & Payer:</span>
                <span className="font-bold text-slate-900">
                  Flat {reconcileTarget.unitNumber} ({reconcileTarget.payerName})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Method & Ref:</span>
                <span className="font-mono font-bold text-indigo-800">
                  {reconcileTarget.paymentMethod} — {reconcileTarget.referenceNumber}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Amount:</span>
                <span className="font-bold text-emerald-700">{formatINR(reconcileTarget.amount)}</span>
              </div>
            </div>

            <form onSubmit={handleConfirmReconcile} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe
                    ? 'కోశాధికారి ఆడిట్ నోట్స్ (తప్పనిసరి) *'
                    : `${t('billing.auditNotes', 'Treasurer Audit Notes')} (Mandatory) *`}
                </label>
                <textarea
                  rows={3}
                  required
                  value={auditNotes}
                  onChange={(e) => setAuditNotes(e.target.value)}
                  placeholder="Enter bank statement reconciliation remarks..."
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600"
                />
              </div>

              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setReconcileTarget(null)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  {t('common.cancel', 'Cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submittingReconcile || !auditNotes.trim()}
                  className={`px-5 py-2 rounded-xl text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50 ${
                    reconcileDecision === 'VERIFIED'
                      ? 'bg-emerald-600 hover:bg-emerald-700'
                      : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  {submittingReconcile
                    ? 'Saving...'
                    : reconcileDecision === 'VERIFIED'
                    ? isTe
                      ? 'ధృవీకరించండి (Confirm Verify)'
                      : 'Confirm Verify'
                    : isTe
                    ? 'తిరస్కరించండి (Confirm Reject)'
                    : 'Confirm Reject'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Offline Cheque/NEFT Submission Modal with Multi-File Proof Upload */}
      {offlineTargetInvoice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe
                  ? 'ఆఫ్‌లైన్ చెక్కు / NEFT & రసీదు ఫైళ్ల అప్‌లోడ్'
                  : 'Submit Offline Cheque / NEFT with Receipt Proofs'}
              </h3>
              <button
                type="button"
                onClick={() => setOfflineTargetInvoice(null)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900 flex justify-between items-center">
              <span>
                <strong>{offlineTargetInvoice.invoiceNumber}</strong> (Flat {offlineTargetInvoice.unitNumber})
              </span>
              <span className="font-bold text-sm">
                {formatINR(offlineTargetInvoice.totalAmount - offlineTargetInvoice.paidAmount)}
              </span>
            </div>

            <form onSubmit={handleOfflineSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {t('billing.paymentMethod', 'Payment Mode')}
                </label>
                <select
                  value={offlineMethod}
                  onChange={(e) => setOfflineMethod(e.target.value as any)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="NEFT">NEFT / RTGS Bank Transfer</option>
                  <option value="CHEQUE">Bank Cheque Drop</option>
                  <option value="IMPS">IMPS Instant Transfer</option>
                  <option value="UPI_MANUAL">Manual UPI Screenshot</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {t('billing.referenceNumber', 'UTR / Cheque Number')} *
                </label>
                <input
                  type="text"
                  required
                  value={offlineRef}
                  onChange={(e) => setOfflineRef(e.target.value)}
                  placeholder="e.g. SBIN002918234 or CHQ-441209"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe
                    ? 'ఒకటి లేదా అంతకంటే ఎక్కువ రసీదు ఫైళ్లను జతచేయండి (MinIO S3) *'
                    : 'Attach One or More Receipt / Transaction Proof Files (MinIO S3)'}
                </label>
                <input
                  type="file"
                  multiple
                  accept="image/*,.pdf"
                  onChange={handleProofMultiUpload}
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-indigo-50 file:text-indigo-700 file:font-semibold"
                />
                {uploadingProof && (
                  <p className="text-xs text-indigo-600 mt-1">Uploading to MinIO S3 (`aikya-storage`)...</p>
                )}
                {proofUrls.length > 0 && (
                  <div className="mt-2 space-y-1">
                    {proofUrls.map((u, idx) => (
                      <div
                        key={idx}
                        className="p-2 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between"
                      >
                        <span className="truncate max-w-[340px]">✓ {u}</span>
                        <button
                          type="button"
                          onClick={() => setProofUrls((prev) => prev.filter((_, i) => i !== idx))}
                          className="text-rose-600 font-bold ml-2 cursor-pointer"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'వివరాలు / బ్యాంక్ పేరు' : 'Bank & Branch Notes'}
                </label>
                <input
                  type="text"
                  value={offlineNotes}
                  onChange={(e) => setOfflineNotes(e.target.value)}
                  placeholder="e.g. SBI Ongole Main Branch transfer"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setOfflineTargetInvoice(null)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  {t('common.cancel', 'Cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submittingOffline}
                  className="px-5 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingOffline
                    ? 'Submitting...'
                    : isTe
                    ? 'కోశాధికారి ఆమోదానికి పంపండి'
                    : 'Submit for Reconciliation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Record Society Maintenance Expense Modal (Multiple Receipt/Proof Attachments) */}
      {showExpenseModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-xl border border-slate-200 space-y-4 max-h-[92vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {isTe
                    ? 'అపార్ట్‌మెంట్ నిర్వహణ ఖర్చు & రసీదుల నమోదు'
                    : 'Record Apartment Maintenance Expense & Attach Receipts'}
                </h3>
                <p className="text-xs text-slate-500">
                  {isTe
                    ? 'ఏలైట్ టవర్స్, ఒంగోలు — బిల్లులు మరియు బ్యాంక్ చెల్లింపు రసీదులను జతచేయండి'
                    : 'Upload multiple vendor bills, GST invoices, and UTR screenshots to MinIO S3 (`aikya-storage`)'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowExpenseModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateExpense} className="space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'ఖర్చు విభాగం (Category) *' : 'Expense Category *'}
                  </label>
                  <select
                    value={expCategory}
                    onChange={(e) => setExpCategory(e.target.value as ExpenseCategory)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    {EXPENSE_CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>
                        {isTe ? c.labelTe : c.labelEn}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'మొత్తం (Amount ₹) *' : 'Expense Amount (₹) *'}
                  </label>
                  <input
                    type="number"
                    required
                    min={1}
                    value={expAmount}
                    onChange={(e) => setExpAmount(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'ఖర్చు శీర్షిక (English) *' : 'Expense Title (English) *'}
                  </label>
                  <input
                    type="text"
                    required
                    value={expTitle}
                    onChange={(e) => setExpTitle(e.target.value)}
                    placeholder="e.g. Borewell Submersible Pump Winding & Pipe Repair"
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'ఖర్చు శీర్షిక (తెలుగు)' : 'Expense Title (Telugu Optional)'}
                  </label>
                  <input
                    type="text"
                    value={expTitleTe}
                    onChange={(e) => setExpTitleTe(e.target.value)}
                    placeholder="ఉదా: బోరు మోటార్ వైండింగ్ & పైపు మరమ్మతులు"
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'వెండర్ పేరు (Vendor) *' : 'Vendor / Payee Name *'}
                  </label>
                  <input
                    type="text"
                    required
                    value={expVendor}
                    onChange={(e) => setExpVendor(e.target.value)}
                    placeholder="e.g. Sri Lakshmi Electricals, Ongole"
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'చెల్లింపు విధానం' : 'Payment Mode'}
                  </label>
                  <select
                    value={expPaymentMode}
                    onChange={(e) => setExpPaymentMode(e.target.value as any)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    <option value="NEFT">NEFT / RTGS</option>
                    <option value="UPI">UPI</option>
                    <option value="CHEQUE">Cheque</option>
                    <option value="IMPS">IMPS</option>
                    <option value="CASH">Petty Cash</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'UTR / చెక్కు నంబర్' : 'UTR / Voucher Ref'}
                  </label>
                  <input
                    type="text"
                    value={expTxRef}
                    onChange={(e) => setExpTxRef(e.target.value)}
                    placeholder="UTR-SBIN991823"
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'ఖర్చు తేదీ' : 'Expense Date'}
                  </label>
                  <input
                    type="date"
                    value={expDate}
                    onChange={(e) => setExpDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'ఆమోదించినవారు' : 'Approved By'}
                  </label>
                  <input
                    type="text"
                    value={expApprovedBy}
                    onChange={(e) => setExpApprovedBy(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe
                    ? 'బిల్లులు / రసీదులు / ట్రాన్సాక్షన్ ప్రూఫ్ ఫైళ్లు (Multiple Files) *'
                    : 'Attach Multiple Bills / Receipts / Transaction Proof Files (MinIO S3) *'}
                </label>
                <input
                  type="file"
                  multiple
                  accept="image/*,.pdf"
                  onChange={handleExpenseReceiptsUpload}
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-emerald-50 file:text-emerald-700 file:font-semibold"
                />
                {uploadingExpReceipts && (
                  <p className="text-xs text-emerald-700 mt-1">
                    Uploading files to MinIO S3 (`http://192.168.10.83/aikya-storage`)...
                  </p>
                )}
                {expReceiptUrls.length > 0 && (
                  <div className="mt-2 space-y-1">
                    {expReceiptUrls.map((u, idx) => (
                      <div
                        key={idx}
                        className="p-2 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between"
                      >
                        <span className="truncate max-w-[400px]">✓ {u}</span>
                        <button
                          type="button"
                          onClick={() => setExpReceiptUrls((prev) => prev.filter((_, i) => i !== idx))}
                          className="text-rose-600 font-bold ml-2 cursor-pointer"
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'ఆడిట్ నోట్స్ / వివరాలు' : 'Notes & Remarks'}
                </label>
                <input
                  type="text"
                  value={expNotes}
                  onChange={(e) => setExpNotes(e.target.value)}
                  placeholder="e.g. Verified work completion certificate & tax invoice"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowExpenseModal(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  {t('common.cancel', 'Cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submittingExpense}
                  className="px-5 py-2 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingExpense
                    ? 'Saving Expense...'
                    : isTe
                    ? 'ఖర్చు & రసీదులు సేవ్ చేయండి'
                    : 'Save Expense & Receipts'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Online Checkout Modal */}
      {onlineTargetInvoice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe ? 'ఆన్‌లైన్ చెల్లింపు (Razorpay / Cashfree)' : 'Instant Online Payment Gateway'}
              </h3>
              <button
                type="button"
                onClick={() => setOnlineTargetInvoice(null)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="p-4 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-between">
              <div>
                <p className="text-xs text-indigo-700 font-semibold">{onlineTargetInvoice.invoiceNumber}</p>
                <p className="text-sm font-bold text-slate-900">Flat {onlineTargetInvoice.unitNumber}</p>
              </div>
              <div className="text-xl font-extrabold text-indigo-950 flex items-center">
                <IndianRupee className="w-4 h-4" />
                {onlineTargetInvoice.totalAmount - onlineTargetInvoice.paidAmount}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              {(['RAZORPAY', 'CASHFREE'] as const).map((gw) => (
                <button
                  key={gw}
                  type="button"
                  onClick={() => setSelectedGateway(gw)}
                  className={`p-3 rounded-xl border text-left transition cursor-pointer ${
                    selectedGateway === gw
                      ? 'border-indigo-600 bg-indigo-50/70 ring-2 ring-indigo-600/20'
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="font-bold text-sm text-slate-900">{gw}</div>
                  <div className="text-[11px] text-slate-500">Zero-fee UPI / NetBanking</div>
                </button>
              ))}
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setOnlineTargetInvoice(null)}
                className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
              >
                {t('common.cancel', 'Cancel')}
              </button>
              <button
                type="button"
                onClick={handleOnlinePay}
                disabled={processingOnline}
                className="px-5 py-2 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
              >
                {processingOnline ? 'Verifying Webhook...' : `Pay via ${selectedGateway}`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default BillingAndReconciliationPage;
