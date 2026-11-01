import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Megaphone,
  MessageSquare,
  Send,
  Pin,
  Plus,
  Sparkles,
  CheckCircle2,
  Globe,
  Smartphone,
} from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { apiClient } from '../lib/apiClient';
import type { NoticeDto, NotificationLogDto, LocaleCode } from '../types/api';

function unwrap<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in (res as Record<string, unknown>)) {
    return (res as { data: T }).data;
  }
  return res as T;
}

const TEMPLATE_PRESETS: Record<string, Record<string, string>> = {
  OTP_LOGIN: {
    otpCode: '482910',
  },
  INVOICE_GENERATED: {
    invoiceNumber: 'INV-ELITE-202611-A102',
    unitNumber: 'A-102',
    amount: '6,050',
    dueDate: '2026-11-10',
  },
  PAYMENT_RECONCILED: {
    amount: '6,050',
    referenceNumber: 'SBIN26305981234',
    unitNumber: 'A-102',
    decision: 'VERIFIED',
    auditNotes: 'Verified against Elite Towers Ongole SBI account statement',
  },
  TICKET_STATUS_UPDATED: {
    ticketNumber: 'TKT-ELITE-101',
    category: 'PLUMBING',
    status: 'IN_PROGRESS',
    technician: 'Mastan Vali (+919848099101)',
  },
};

export const NoticesAndNotificationsPage: React.FC = () => {
  const { t } = useTranslation();
  const { locale, activeAssociation } = useAppStore();
  const isTe = locale === 'te';

  const [notices, setNotices] = useState<NoticeDto[]>([]);
  const [logs, setLogs] = useState<NotificationLogDto[]>([]);
  const [banner, setBanner] = useState<string | null>(null);

  // Publish Notice Modal State
  const [showPublishModal, setShowPublishModal] = useState<boolean>(false);
  const [titleEn, setTitleEn] = useState<string>('');
  const [titleTe, setTitleTe] = useState<string>('');
  const [contentEn, setContentEn] = useState<string>('');
  const [contentTe, setContentTe] = useState<string>('');
  const [category, setCategory] = useState<NoticeDto['category']>('MAINTENANCE');
  const [priority, setPriority] = useState<NoticeDto['priority']>('HIGH');
  const [pinned, setPinned] = useState<boolean>(true);
  const [submittingNotice, setSubmittingNotice] = useState<boolean>(false);

  // Live Template Previewer State
  const [templateKey, setTemplateKey] = useState<string>('INVOICE_GENERATED');
  const [previewLocale, setPreviewLocale] = useState<LocaleCode>('te');
  const [channel, setChannel] = useState<'WHATSAPP' | 'SMS'>('WHATSAPP');
  const [recipientName, setRecipientName] = useState<string>('Lakshmi Devi Konidela');
  const [recipientPhone, setRecipientPhone] = useState<string>('+919848022222');
  const [renderedPreview, setRenderedPreview] = useState<string>('');
  const [dispatchingPreview, setDispatchingPreview] = useState<boolean>(false);

  const loadNoticesAndLogs = useCallback(async () => {
    try {
      const [nRes, lRes] = await Promise.all([
        apiClient.getNotices(),
        apiClient.getNotificationLogs(),
      ]);
      setNotices(unwrap<NoticeDto[]>(nRes) || []);
      setLogs(unwrap<NotificationLogDto[]>(lRes) || []);
    } catch {
      // handled by apiClient fallback
    }
  }, [activeAssociation?.schemaName]);

  useEffect(() => {
    loadNoticesAndLogs();
  }, [loadNoticesAndLogs]);

  const handlePublishNotice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!titleEn.trim() || !contentEn.trim()) return;
    setSubmittingNotice(true);
    try {
      await apiClient.createNotice({
        title: titleEn.trim(),
        titleTe: titleTe.trim() || titleEn.trim(),
        content: contentEn.trim(),
        contentTe: contentTe.trim() || contentEn.trim(),
        category,
        priority,
        pinned,
      });
      setBanner(
        isTe
          ? 'కొత్త ద్విభాషా నోటీసు బోర్డుపై ప్రచురించబడింది!'
          : 'Bilingual community notice published to Elite Towers Notice Board!'
      );
      setShowPublishModal(false);
      setTitleEn('');
      setTitleTe('');
      setContentEn('');
      setContentTe('');
      await loadNoticesAndLogs();
    } finally {
      setSubmittingNotice(false);
    }
  };

  const handlePreviewAndDispatch = async (e: React.FormEvent) => {
    e.preventDefault();
    setDispatchingPreview(true);
    try {
      const vars = TEMPLATE_PRESETS[templateKey] || {};
      const res = await apiClient.post<NotificationLogDto>('/api/v1/notifications/preview', {
        templateKey,
        locale: previewLocale,
        channel,
        recipientPhone,
        recipientName,
        variables: vars,
        parameters: vars,
      });
      const data = unwrap<NotificationLogDto>(res);
      setRenderedPreview(data?.renderedMessage || '');
      await loadNoticesAndLogs();
    } finally {
      setDispatchingPreview(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Megaphone className="w-6 h-6 text-orange-600" />
            {isTe
              ? 'ద్విభాషా నోటీసు బోర్డు & తెలుగు/ఇంగ్లీష్ SMS/WhatsApp ఇంజిన్'
              : t('nav.notices', 'Bilingual Notice Board & Localized SMS/WhatsApp Engine')}
          </h1>
          <p className="text-sm text-slate-600 mt-0.5">
            {isTe
              ? 'ఏలైట్ టవర్స్, ఒంగోలు ప్రకటనలు మరియు Thymeleaf తెలుగు/ఇంగ్లీష్ SMS/WhatsApp టెంప్లేట్ ప్రివ్యూయర్'
              : 'Community announcements in English & Telugu alongside the live Thymeleaf SMS/WhatsApp notification engine.'}
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowPublishModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold shadow-sm transition cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          {isTe ? 'కొత్త నోటీసు ప్రచురించండి' : t('notices.publishNotice', 'Publish Bilingual Notice')}
        </button>
      </div>

      {banner && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-900 flex items-center justify-between text-sm font-medium">
          <span className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            {banner}
          </span>
          <button
            type="button"
            onClick={() => setBanner(null)}
            className="text-xs underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left 6 cols: Bilingual Community Notice Board */}
        <div className="lg:col-span-6 space-y-4">
          <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <Pin className="w-4 h-4 text-orange-600" />
            {isTe
              ? 'కమ్యూనిటీ నోటీసు బోర్డు (Bilingual Feed)'
              : 'Community Notice Board (English & Telugu)'}
          </h2>

          {notices.map((n) => (
            <div
              key={n.id}
              className="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-xs space-y-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-800">
                    {n.category}
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                      n.priority === 'URGENT' || n.priority === 'HIGH'
                        ? 'bg-rose-100 text-rose-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {n.priority}
                  </span>
                </div>
                {n.pinned && (
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-orange-600">
                    <Pin className="w-3.5 h-3.5" /> Pinned
                  </span>
                )}
              </div>

              <h3 className="text-base font-bold text-slate-900">
                {isTe ? n.titleTe || n.title : n.title}
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                {isTe ? n.contentTe || n.content : n.content}
              </p>

              {/* Secondary language subtitle */}
              <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100 text-xs text-slate-500">
                <strong className="text-slate-700">{isTe ? 'EN:' : 'తెలుగు:'}</strong>{' '}
                {isTe ? n.title : n.titleTe || n.title}
              </div>

              <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                <span>{n.publishedByName}</span>
                <span>{new Date(n.publishedAt).toLocaleDateString('en-IN')}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Right 6 cols: Live Localized SMS/WhatsApp Template Previewer & Dispatch Log */}
        <div className="lg:col-span-6 space-y-5">
          {/* Live Template Previewer Card */}
          <div className="bg-gradient-to-br from-indigo-950 via-indigo-900 to-slate-900 rounded-2xl p-6 text-white shadow-md space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-amber-300" />
                <h2 className="text-base font-bold">
                  {isTe
                    ? 'లైవ్ Thymeleaf తెలుగు/ఇంగ్లీష్ SMS & WhatsApp టెంప్లేట్ ఇంజిన్'
                    : t(
                        'notices.templatePreviewer',
                        'Live Thymeleaf Localized SMS / WhatsApp Template Previewer'
                      )}
                </h2>
              </div>
              <Globe className="w-5 h-5 text-indigo-300" />
            </div>

            <form onSubmit={handlePreviewAndDispatch} className="space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-[11px] font-bold uppercase text-indigo-200 mb-1">
                    Template Key
                  </label>
                  <select
                    value={templateKey}
                    onChange={(e) => setTemplateKey(e.target.value)}
                    className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-xs text-white"
                  >
                    <option value="OTP_LOGIN">OTP_LOGIN</option>
                    <option value="INVOICE_GENERATED">INVOICE_GENERATED</option>
                    <option value="PAYMENT_RECONCILED">PAYMENT_RECONCILED</option>
                    <option value="TICKET_STATUS_UPDATED">TICKET_STATUS_UPDATED</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] font-bold uppercase text-indigo-200 mb-1">
                    Render Locale
                  </label>
                  <select
                    value={previewLocale}
                    onChange={(e) => setPreviewLocale(e.target.value as LocaleCode)}
                    className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-xs text-white"
                  >
                    <option value="te">తెలుగు (te)</option>
                    <option value="en">English (en)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] font-bold uppercase text-indigo-200 mb-1">
                    Channel
                  </label>
                  <select
                    value={channel}
                    onChange={(e) => setChannel(e.target.value as 'WHATSAPP' | 'SMS')}
                    className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-2 text-xs text-white"
                  >
                    <option value="WHATSAPP">WHATSAPP</option>
                    <option value="SMS">SMS</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold uppercase text-indigo-200 mb-1">
                    Recipient Name
                  </label>
                  <input
                    type="text"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-1.5 text-xs text-white"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold uppercase text-indigo-200 mb-1">
                    Recipient Phone
                  </label>
                  <input
                    type="text"
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    className="w-full rounded-lg bg-slate-900/90 border border-indigo-400/30 px-2.5 py-1.5 text-xs text-white"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={dispatchingPreview}
                className="w-full py-2.5 px-4 rounded-xl bg-orange-500 hover:bg-orange-600 text-white font-semibold text-xs flex items-center justify-center gap-2 shadow cursor-pointer disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                {dispatchingPreview
                  ? 'Rendering via Thymeleaf...'
                  : isTe
                  ? 'టెంప్లేట్ రెండర్ చేసి SMS/WhatsApp పంపండి'
                  : 'Render & Dispatch Localized Notification'}
              </button>
            </form>

            {renderedPreview && (
              <div className="p-3.5 rounded-xl bg-emerald-950/90 border border-emerald-500/40 text-emerald-100 text-xs space-y-1">
                <div className="font-bold text-emerald-300 flex items-center gap-1.5">
                  <Smartphone className="w-3.5 h-3.5" />
                  Rendered Output ({templateKey} • {previewLocale.toUpperCase()}):
                </div>
                <p className="leading-relaxed font-medium">{renderedPreview}</p>
              </div>
            )}
          </div>

          {/* Notification Dispatch Log */}
          <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-emerald-600" />
                {isTe
                  ? 'SMS / WhatsApp పంపిన సందేశాల లాగ్'
                  : t('notices.notificationLogs', 'Localized SMS / WhatsApp Dispatch Log')}
              </h3>
              <span className="text-xs font-bold text-slate-400">{logs.length} dispatches</span>
            </div>

            <div className="divide-y divide-slate-100 max-h-96 overflow-y-auto">
              {logs.map((log) => (
                <div key={log.id} className="p-4 space-y-1.5 hover:bg-slate-50 transition">
                  <div className="flex items-center justify-between text-xs">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                          log.channel === 'WHATSAPP'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-blue-100 text-blue-800'
                        }`}
                      >
                        {log.channel}
                      </span>
                      <span className="font-mono font-bold text-slate-800">{log.templateKey}</span>
                      <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-900 font-bold text-[10px] uppercase">
                        {log.locale}
                      </span>
                    </div>
                    <span className="text-[11px] text-emerald-700 font-semibold">{log.status}</span>
                  </div>

                  <p className="text-xs text-slate-800 bg-slate-50 p-2.5 rounded-xl border border-slate-200/70 leading-relaxed">
                    {log.renderedMessage}
                  </p>

                  <div className="flex items-center justify-between text-[11px] text-slate-400">
                    <span>
                      To: {log.recipientName} ({log.recipientPhone})
                    </span>
                    <span>
                      {log.createdAt ? new Date(log.createdAt).toLocaleTimeString('en-IN') : ''}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Publish Bilingual Notice Modal */}
      {showPublishModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe ? 'కొత్త ద్విభాషా నోటీసు ప్రచురణ' : 'Publish Bilingual Community Notice'}
              </h3>
              <button
                type="button"
                onClick={() => setShowPublishModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handlePublishNotice} className="space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as NoticeDto['category'])}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    <option value="MAINTENANCE">MAINTENANCE</option>
                    <option value="EVENT">EVENT</option>
                    <option value="EMERGENCY">EMERGENCY</option>
                    <option value="MEETING">MEETING</option>
                    <option value="GENERAL">GENERAL</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as NoticeDto['priority'])}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    <option value="NORMAL">NORMAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="URGENT">URGENT</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Title (English) *
                </label>
                <input
                  type="text"
                  required
                  value={titleEn}
                  onChange={(e) => setTitleEn(e.target.value)}
                  placeholder="e.g. Borewell Pump Maintenance on Friday"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  శీర్షిక (Telugu)
                </label>
                <input
                  type="text"
                  value={titleTe}
                  onChange={(e) => setTitleTe(e.target.value)}
                  placeholder="ఉదా: శుక్రవారం బోరు మోటార్ మరమ్మతు పనులు"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Message Content (English) *
                </label>
                <textarea
                  rows={2}
                  required
                  value={contentEn}
                  onChange={(e) => setContentEn(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  సందేశ వివరాలు (Telugu)
                </label>
                <textarea
                  rows={2}
                  value={contentTe}
                  onChange={(e) => setContentTe(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={pinned}
                  onChange={(e) => setPinned(e.target.checked)}
                />
                <span>Pin to top of Dashboard & Notice Board</span>
              </label>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowPublishModal(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingNotice}
                  className="px-5 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingNotice ? 'Publishing...' : 'Publish Notice'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default NoticesAndNotificationsPage;
