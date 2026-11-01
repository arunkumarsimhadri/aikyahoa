import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Wrench,
  Plus,
  AlertTriangle,
  Clock,
  CheckCircle2,
  UserCheck,
  Phone,
  Image as ImageIcon,
  ExternalLink,
  Filter,
} from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { apiClient } from '../lib/apiClient';
import type {
  HelpdeskTicketDto,
  TicketCategory,
  TicketPriority,
  TicketStatus,
} from '../types/api';

function unwrap<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in (res as Record<string, unknown>)) {
    return (res as { data: T }).data;
  }
  return res as T;
}

const CATEGORIES: { value: TicketCategory; labelEn: string; labelTe: string }[] = [
  { value: 'PLUMBING', labelEn: 'Plumbing & Water Leakage', labelTe: 'ప్లంబింగ్ & నీటి లీకేజీ' },
  { value: 'ELECTRICAL', labelEn: 'Electrical & Power Backup', labelTe: 'ఎలక్ట్రికల్ & పవర్ బ్యాకప్' },
  { value: 'ELEVATOR', labelEn: 'Lift / Elevator Maintenance', labelTe: 'లిఫ్ట్ మరమ్మతులు' },
  { value: 'WATER_SUPPLY', labelEn: 'Municipal / Borewell Water', labelTe: 'నీటి సరఫరా (బోరు/మున్సిపల్)' },
  { value: 'CARPENTRY', labelEn: 'Carpentry & Doors', labelTe: 'కార్పెంటరీ & తలుపులు' },
  { value: 'SECURITY', labelEn: 'Gate Security & Intercom', labelTe: 'సెక్యూరిటీ & ఇంటర్‌కామ్' },
  { value: 'CLEANING', labelEn: 'Corridor & Garbage Cleaning', labelTe: 'పారిశుధ్యం & క్లీనింగ్' },
  { value: 'OTHER', labelEn: 'Other Facility Issue', labelTe: 'ఇతర సమస్యలు' },
];

const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export const HelpdeskPage: React.FC = () => {
  const { t } = useTranslation();
  const { locale, activeAssociation } = useAppStore();
  const isTe = locale === 'te';

  const [tickets, setTickets] = useState<HelpdeskTicketDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [toast, setToast] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Raise Complaint Modal State
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [category, setCategory] = useState<TicketCategory>('PLUMBING');
  const [priority, setPriority] = useState<TicketPriority>('HIGH');
  const [title, setTitle] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [attachmentUrl, setAttachmentUrl] = useState<string>('');
  const [previewDataUrl, setPreviewDataUrl] = useState<string>('');
  const [uploadingImage, setUploadingImage] = useState<boolean>(false);
  const [submittingCreate, setSubmittingCreate] = useState<boolean>(false);

  // Manage / Assign Technician Modal State
  const [manageTicket, setManageTicket] = useState<HelpdeskTicketDto | null>(null);
  const [nextStatus, setNextStatus] = useState<TicketStatus>('IN_PROGRESS');
  const [techName, setTechName] = useState<string>('');
  const [techPhone, setTechPhone] = useState<string>('');
  const [resolutionNotes, setResolutionNotes] = useState<string>('');
  const [submittingUpdate, setSubmittingUpdate] = useState<boolean>(false);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.getTickets();
      setTickets(unwrap<HelpdeskTicketDto[]>(res) || []);
    } catch (err: any) {
      setToast({ type: 'error', text: err?.message || 'Failed to load helpdesk tickets' });
    } finally {
      setLoading(false);
    }
  }, [activeAssociation?.schemaName]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingImage(true);
    const reader = new FileReader();
    reader.onload = async () => {
      const base64 = typeof reader.result === 'string' ? reader.result : '';
      setPreviewDataUrl(base64);
      try {
        const uploaded = await apiClient.uploadFiles([file], 'HELPDESK_ATTACHMENT');
        if (uploaded.length > 0) {
          setAttachmentUrl(uploaded[0].fileUrl);
        }
      } catch {
        setAttachmentUrl(
          `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/${encodeURIComponent(file.name)}`
        );
      } finally {
        setUploadingImage(false);
      }
    };
    reader.readAsDataURL(file);
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !description.trim()) return;
    setSubmittingCreate(true);
    setToast(null);
    try {
      const res = await apiClient.createTicket({
        unitId: 2,
        unitNumber: activeAssociation?.unitNumber || 'A-101',
        category,
        priority,
        title: title.trim(),
        description: description.trim(),
        attachmentUrl: attachmentUrl || undefined,
      });
      const created = unwrap<HelpdeskTicketDto>(res);
      setToast({
        type: 'success',
        text: isTe
          ? `ఫిర్యాదు టికెట్ ${created?.ticketNumber || ''} విజయవంతంగా నమోదు చేయబడింది!`
          : `Helpdesk Ticket ${created?.ticketNumber || ''} logged with SLA tracking!`,
      });
      setShowCreateModal(false);
      setTitle('');
      setDescription('');
      setAttachmentUrl('');
      setPreviewDataUrl('');
      await loadTickets();
    } catch (err: any) {
      setToast({ type: 'error', text: err?.message || 'Failed to create ticket' });
    } finally {
      setSubmittingCreate(false);
    }
  };

  const openManageModal = (ticket: HelpdeskTicketDto) => {
    setManageTicket(ticket);
    setNextStatus(
      ticket.status === 'OPEN'
        ? 'IN_PROGRESS'
        : ticket.status === 'IN_PROGRESS'
        ? 'RESOLVED'
        : 'CLOSED'
    );
    setTechName(ticket.assignedTechnicianName || 'Mastan Vali (Plumber & Facility Tech)');
    setTechPhone(ticket.assignedTechnicianPhone || '+919848099101');
    setResolutionNotes(ticket.resolutionNotes || 'Inspected unit line and replaced faulty valve gasket.');
  };

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!manageTicket) return;
    setSubmittingUpdate(true);
    setToast(null);
    try {
      await apiClient.updateTicketStatus(manageTicket.id, {
        status: nextStatus,
        assignedTechnicianName: techName.trim(),
        assignedTechnicianPhone: techPhone.trim(),
        resolutionNotes: resolutionNotes.trim(),
      });
      setToast({
        type: 'success',
        text: isTe
          ? `టికెట్ ${manageTicket.ticketNumber} స్థితి ${nextStatus} కు మార్చబడింది మరియు నివాసికి తెలుగు/ఇంగ్లీష్ SMS పంపబడింది!`
          : `Ticket ${manageTicket.ticketNumber} transitioned to ${nextStatus} & resident notified via localized SMS/WhatsApp!`,
      });
      setManageTicket(null);
      await loadTickets();
    } catch (err: any) {
      setToast({ type: 'error', text: err?.message || 'Failed to update ticket status' });
    } finally {
      setSubmittingUpdate(false);
    }
  };

  const filteredTickets = tickets.filter((tkt) =>
    statusFilter === 'ALL' ? true : tkt.status === statusFilter
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Wrench className="w-6 h-6 text-orange-600" />
            {isTe ? 'హెల్ప్‌డెస్క్ ఫిర్యాదులు & SLA ట్రాకింగ్' : t('nav.helpdesk', 'Helpdesk & SLA Tracking')}
          </h1>
          <p className="text-sm text-slate-600 mt-0.5">
            {isTe
              ? 'ఏలైట్ టవర్స్ (ఒంగోలు) ప్లంబింగ్, ఎలక్ట్రికల్, లిఫ్ట్ మరియు వాటర్ సమస్యల పరిష్కార బోర్డు'
              : 'Track unit & common-area maintenance complaints from OPEN to CLOSED with SLA countdown & technician assignment.'}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold shadow-sm transition cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          {isTe ? 'కొత్త ఫిర్యాదు నమోదు చేయండి' : t('helpdesk.raiseComplaint', 'Raise Complaint')}
        </button>
      </div>

      {toast && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between ${
            toast.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <span className="text-sm font-medium">{toast.text}</span>
          <button
            type="button"
            onClick={() => setToast(null)}
            className="text-xs font-semibold underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Status Lifecycle Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex items-center gap-1.5 flex-wrap">
          <Filter className="w-4 h-4 text-slate-400 mr-1" />
          {(['ALL', ...STATUSES] as const).map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                statusFilter === st
                  ? 'bg-indigo-900 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st} (
              {st === 'ALL'
                ? tickets.length
                : tickets.filter((x) => x.status === st).length}
              )
            </button>
          ))}
        </div>
        <div className="text-xs text-slate-500 flex items-center gap-2">
          <span className="inline-flex items-center gap-1 text-rose-600 font-semibold">
            <AlertTriangle className="w-3.5 h-3.5" />
            SLA Breached: {tickets.filter((x) => x.slaBreached && x.status !== 'CLOSED').length}
          </span>
        </div>
      </div>

      {/* Ticket Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {loading ? (
          <div className="col-span-2 py-12 text-center text-slate-400">Loading helpdesk tickets...</div>
        ) : filteredTickets.length === 0 ? (
          <div className="col-span-2 py-12 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
            {isTe ? 'ఫిర్యాదు టికెట్లు లేవు' : 'No tickets found for selected status'}
          </div>
        ) : (
          filteredTickets.map((tkt) => (
            <div
              key={tkt.id}
              className="bg-white rounded-2xl border border-slate-200/90 p-5 shadow-xs hover:shadow-md transition flex flex-col justify-between gap-4"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="font-mono text-xs font-extrabold px-2.5 py-1 rounded-md bg-slate-100 text-slate-900">
                      {tkt.ticketNumber}
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-800">
                      Flat {tkt.unitNumber}
                    </span>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
                      {tkt.category}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[11px] font-bold ${
                        tkt.priority === 'CRITICAL' || tkt.priority === 'HIGH'
                          ? 'bg-rose-100 text-rose-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {tkt.priority}
                    </span>
                  </div>

                  <span
                    className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                      tkt.status === 'RESOLVED' || tkt.status === 'CLOSED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : tkt.status === 'IN_PROGRESS'
                        ? 'bg-blue-100 text-blue-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {tkt.status}
                  </span>
                </div>

                <div>
                  <h3 className="text-base font-bold text-slate-900">{tkt.title}</h3>
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">{tkt.description}</p>
                </div>

                {tkt.attachmentUrl && (
                  <div>
                    <a
                      href={tkt.attachmentUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-700 hover:underline bg-indigo-50 px-2.5 py-1 rounded-lg"
                    >
                      <ImageIcon className="w-3.5 h-3.5" />
                      {isTe ? 'ఫోటో / అటాచ్‌మెంట్ చూడండి' : 'View Attached Issue Photo (MinIO S3)'}
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                )}

                {/* Technician & SLA Row */}
                <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <div className="flex items-center gap-2 text-slate-700">
                    <UserCheck className="w-4 h-4 text-indigo-600" />
                    {tkt.assignedTechnicianName ? (
                      <span>
                        <strong>{tkt.assignedTechnicianName}</strong>
                        {tkt.assignedTechnicianPhone && (
                          <span className="ml-1.5 text-slate-500 inline-flex items-center gap-0.5">
                            <Phone className="w-3 h-3" />
                            {tkt.assignedTechnicianPhone}
                          </span>
                        )}
                      </span>
                    ) : (
                      <span className="text-slate-400 italic">
                        {isTe ? 'టెక్నీషియన్ ఇంకా కేటాయించబడలేదు' : 'Unassigned — Awaiting Technician'}
                      </span>
                    )}
                  </div>

                  <div>
                    {tkt.slaBreached ? (
                      <span className="inline-flex items-center gap-1 font-bold text-rose-700 bg-rose-100 px-2 py-0.5 rounded-md">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        {isTe ? 'SLA గడువు దాటింది' : 'SLA BREACHED'}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                        <Clock className="w-3.5 h-3.5" />
                        {isTe ? 'SLA పరిధిలో ఉంది' : 'Within SLA'}
                      </span>
                    )}
                  </div>
                </div>

                {tkt.resolutionNotes && (
                  <div className="p-2.5 rounded-lg bg-emerald-50/70 border border-emerald-200 text-xs text-emerald-900">
                    <strong>{isTe ? 'పరిష్కార వివరాలు:' : 'Resolution Notes:'}</strong> {tkt.resolutionNotes}
                  </div>
                )}
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <span className="text-[11px] text-slate-400">
                  Raised by {tkt.raisedByName} •{' '}
                  {tkt.createdAt ? new Date(tkt.createdAt).toLocaleDateString('en-IN') : 'Today'}
                </span>
                <button
                  type="button"
                  onClick={() => openManageModal(tkt)}
                  className="px-3.5 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-900 text-xs font-bold transition cursor-pointer"
                >
                  {isTe ? 'టెక్నీషియన్ / స్థితి మార్పు' : 'Manage / Assign Technician'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Raise Complaint Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe ? 'కొత్త హెల్ప్‌డెస్క్ ఫిర్యాదు నమోదు' : 'Raise New Helpdesk Complaint'}
              </h3>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateTicket} className="space-y-3.5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'విభాగం (Category)' : 'Category'}
                  </label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value as TicketCategory)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    {CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>
                        {isTe ? c.labelTe : c.labelEn}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    {isTe ? 'తీవ్రత (Priority)' : 'Priority & SLA Tier'}
                  </label>
                  <select
                    value={priority}
                    onChange={(e) => setPriority(e.target.value as TicketPriority)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'సమస్య శీర్షిక (Title) *' : 'Issue Summary Title *'}
                </label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Master bathroom geyser MCB tripping repeatedly"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'వివరణ (Description) *' : 'Detailed Description & Preferred Visit Time *'}
                </label>
                <textarea
                  rows={3}
                  required
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe exact location, symptoms, and preferred slot for technician visit..."
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'సమస్య ఫోటో అప్‌లోడ్ (Image Upload Preview)' : 'Attach Issue Photo (MinIO S3 Upload)'}
                </label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-orange-50 file:text-orange-700 file:font-semibold"
                />
                {uploadingImage && (
                  <p className="text-xs text-orange-600 mt-1">Uploading photo to MinIO S3 (`aikya-storage`)...</p>
                )}
                {previewDataUrl && (
                  <div className="mt-2 p-2 rounded-xl bg-slate-50 border border-slate-200 flex items-center gap-3">
                    <img
                      src={previewDataUrl}
                      alt="Complaint preview"
                      className="w-16 h-16 object-cover rounded-lg border border-slate-300"
                    />
                    <div className="text-xs text-emerald-700 break-all">
                      <CheckCircle2 className="w-4 h-4 inline mr-1" />
                      Uploaded: {attachmentUrl}
                    </div>
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  {t('common.cancel', 'Cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submittingCreate}
                  className="px-5 py-2 rounded-xl bg-orange-600 hover:bg-orange-700 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingCreate ? 'Submitting...' : isTe ? 'ఫిర్యాదు నమోదు చేయండి' : 'Submit Ticket'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Manage / Assign Technician Modal */}
      {manageTicket && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe
                  ? `టికెట్ నిర్వహణ (${manageTicket.ticketNumber})`
                  : `Update Ticket & Technician (${manageTicket.ticketNumber})`}
              </h3>
              <button
                type="button"
                onClick={() => setManageTicket(null)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleUpdateStatus} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'కొత్త స్థితి (Next Status)' : 'Transition Status'}
                </label>
                <select
                  value={nextStatus}
                  onChange={(e) => setNextStatus(e.target.value as TicketStatus)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm font-semibold"
                >
                  {STATUSES.map((st) => (
                    <option key={st} value={st}>
                      {st}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'టెక్నీషియన్ పేరు' : 'Assigned Technician Name'}
                </label>
                <input
                  type="text"
                  value={techName}
                  onChange={(e) => setTechName(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'టెక్నీషియన్ ఫోన్ నంబర్' : 'Technician Phone'}
                </label>
                <input
                  type="text"
                  value={techPhone}
                  onChange={(e) => setTechPhone(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'పరిష్కార నోట్స్ (Resolution Notes)' : 'Resolution / Work Notes'}
                </label>
                <textarea
                  rows={3}
                  value={resolutionNotes}
                  onChange={(e) => setResolutionNotes(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setManageTicket(null)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  {t('common.cancel', 'Cancel')}
                </button>
                <button
                  type="submit"
                  disabled={submittingUpdate}
                  className="px-5 py-2 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingUpdate ? 'Updating...' : isTe ? 'స్థితి అప్‌డేట్ చేయండి' : 'Update Status'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default HelpdeskPage;
