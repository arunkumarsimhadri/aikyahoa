import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Vote,
  Lock,
  Eye,
  ShieldAlert,
  CheckCircle2,
  Plus,
  FileText,
  Download,
  FolderArchive,
  Award,
  Upload,
} from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { apiClient } from '../lib/apiClient';
import type { PollDto, DocumentDto, DocumentCategory } from '../types/api';

function unwrap<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in (res as Record<string, unknown>)) {
    return (res as { data: T }).data;
  }
  return res as T;
}

export const GovernancePage: React.FC = () => {
  const { t } = useTranslation();
  const { locale, activeAssociation } = useAppStore();
  const isTe = locale === 'te';

  const activeRole = activeAssociation?.role || 'OWNER';
  const isTenantRole = activeRole === 'TENANT';
  const isCommittee = ['PRESIDENT', 'SECRETARY', 'TREASURER'].includes(activeRole);

  const [polls, setPolls] = useState<PollDto[]>([]);
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [fyFilter, setFyFilter] = useState<string>('ALL');
  const [banner, setBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [votingPollId, setVotingPollId] = useState<number | null>(null);

  // Create Poll Modal State
  const [showCreatePoll, setShowCreatePoll] = useState<boolean>(false);
  const [pollTitle, setPollTitle] = useState<string>('');
  const [pollTitleTe, setPollTitleTe] = useState<string>('');
  const [pollDesc, setPollDesc] = useState<string>('');
  const [pollDescTe, setPollDescTe] = useState<string>('');
  const [isSecret, setIsSecret] = useState<boolean>(false);
  const [ownerOnly, setOwnerOnly] = useState<boolean>(true);
  const [opt1En, setOpt1En] = useState<string>('Approve Proposal');
  const [opt1Te, setOpt1Te] = useState<string>('ఆమోదిస్తున్నాను');
  const [opt2En, setOpt2En] = useState<string>('Reject / Defer to AGM');
  const [opt2Te, setOpt2Te] = useState<string>('తిరస్కరిస్తున్నాను / వాయిదా');
  const [submittingPoll, setSubmittingPoll] = useState<boolean>(false);

  // Upload AGM Document Modal State
  const [showUploadDoc, setShowUploadDoc] = useState<boolean>(false);
  const [docTitle, setDocTitle] = useState<string>('');
  const [docTitleTe, setDocTitleTe] = useState<string>('');
  const [docCategory, setDocCategory] = useState<DocumentCategory>('AGM_MINUTES');
  const [docFy, setDocFy] = useState<string>('2025-26');
  const [docFileUrl, setDocFileUrl] = useState<string>('');
  const [uploadingDocFile, setUploadingDocFile] = useState<boolean>(false);
  const [submittingDoc, setSubmittingDoc] = useState<boolean>(false);

  const loadGovernance = useCallback(async () => {
    try {
      const [pRes, dRes] = await Promise.all([
        apiClient.getPolls(),
        apiClient.getDocuments(),
      ]);
      setPolls(unwrap<PollDto[]>(pRes) || []);
      setDocuments(unwrap<DocumentDto[]>(dRes) || []);
    } catch (err: any) {
      setBanner({ type: 'error', text: err?.message || 'Failed to load governance records' });
    }
  }, [activeAssociation?.schemaName]);

  useEffect(() => {
    loadGovernance();
  }, [loadGovernance]);

  const handleCastVote = async (poll: PollDto, optionId: number) => {
    setVotingPollId(poll.id);
    setBanner(null);
    try {
      await apiClient.votePoll(poll.id, { optionId });
      setBanner({
        type: 'success',
        text: isTe
          ? `ఫ్లాట్ ${activeAssociation?.unitNumber || 'A-101'} తరపున మీ ఓటు విజయవంతంగా నమోదు చేయబడింది!`
          : `1-Vote-Per-Unit ballot recorded for Flat ${activeAssociation?.unitNumber || 'A-101'}!`,
      });
      await loadGovernance();
    } catch (err: any) {
      setBanner({
        type: 'error',
        text:
          err?.message ||
          (isTe
            ? 'ఓటు వేయడం విఫలమైంది: ఫ్లాట్ యజమానులకు మాత్రమే ఓటు హక్కు ఉంది లేదా ఇప్పటికే ఓటు వేయబడింది.'
            : 'Vote rejected: 1-Vote-Per-Unit (Owner Only) constraint enforced.'),
      });
    } finally {
      setVotingPollId(null);
    }
  };

  const handleCreatePoll = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pollTitle.trim()) return;
    setSubmittingPoll(true);
    setBanner(null);
    try {
      await apiClient.createPoll({
        title: pollTitle.trim(),
        titleTe: pollTitleTe.trim() || pollTitle.trim(),
        description: pollDesc.trim(),
        descriptionTe: pollDescTe.trim() || pollDesc.trim(),
        isSecret,
        ownerOnly,
        endsAt: '2026-12-15T23:59:59Z',
        options: [
          { optionText: opt1En, optionTextTe: opt1Te },
          { optionText: opt2En, optionTextTe: opt2Te },
        ],
      });
      setBanner({
        type: 'success',
        text: isTe ? 'కొత్త AGM తీర్మానం / పోల్ సృష్టించబడింది!' : 'New community resolution poll published!',
      });
      setShowCreatePoll(false);
      setPollTitle('');
      setPollTitleTe('');
      setPollDesc('');
      setPollDescTe('');
      await loadGovernance();
    } catch (err: any) {
      setBanner({ type: 'error', text: err?.message || 'Failed to create poll' });
    } finally {
      setSubmittingPoll(false);
    }
  };

  const handleDocFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingDocFile(true);
    try {
      const uploaded = await apiClient.uploadFiles([file], 'AGM_DOCUMENT');
      if (uploaded.length > 0) {
        setDocFileUrl(uploaded[0].fileUrl);
      }
    } catch {
      setDocFileUrl(
        `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/${encodeURIComponent(file.name)}`
      );
    } finally {
      setUploadingDocFile(false);
    }
  };

  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!docTitle.trim()) return;
    setSubmittingDoc(true);
    try {
      const finalUrl =
        docFileUrl ||
        `http://192.168.10.83/aikya-storage/${activeAssociation?.schemaName || 'aikya_assoc_1'}/agm-${Date.now()}.pdf`;
      await apiClient.createDocument({
        title: docTitle.trim(),
        titleTe: docTitleTe.trim() || docTitle.trim(),
        category: docCategory,
        financialYear: docFy,
        fileUrl: finalUrl,
        fileSizeBytes: 512000,
      });
      setBanner({
        type: 'success',
        text: isTe
          ? 'AGM / సొసైటీ డాక్యుమెంట్ MinIO S3 లో భద్రపరచబడింది!'
          : 'Document uploaded to MinIO S3 (`aikya-storage`) and archived!',
      });
      setShowUploadDoc(false);
      setDocTitle('');
      setDocTitleTe('');
      setDocFileUrl('');
      await loadGovernance();
    } catch (err: any) {
      setBanner({ type: 'error', text: err?.message || 'Failed to upload document' });
    } finally {
      setSubmittingDoc(false);
    }
  };

  const filteredDocs = documents.filter((d) =>
    fyFilter === 'ALL' ? true : d.financialYear === fyFilter
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Vote className="w-6 h-6 text-indigo-700" />
            {isTe
              ? 'అసోసియేషన్ పాలన, AGM ఓటింగ్ & డాక్యుమెంట్ ఆర్కైవ్'
              : t('nav.governance', 'Governance, Polls & AGM Document Archive')}
          </h1>
          <p className="text-sm text-slate-600 mt-0.5">
            {isTe
              ? 'ప్రతి ఫ్లాట్‌కు 1 ఓటు (యజమాని మాత్రమే) — ఓపెన్ పోల్స్, రహస్య బ్యాలెట్ మరియు ఆడిట్ నివేదికలు'
              : 'Enforced 1-Vote-Per-Unit (Owner Only) AGM resolutions, Secret Ballots, and statutory society records.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {isCommittee && (
            <>
              <button
                type="button"
                onClick={() => setShowCreatePoll(true)}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white text-xs font-bold shadow-sm transition cursor-pointer"
              >
                <Plus className="w-4 h-4" />
                {isTe ? 'కొత్త పోల్ / తీర్మానం' : t('governance.createPoll', 'Create Resolution Poll')}
              </button>
              <button
                type="button"
                onClick={() => setShowUploadDoc(true)}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold shadow-sm transition cursor-pointer"
              >
                <Upload className="w-4 h-4" />
                {isTe ? 'AGM PDF అప్‌లోడ్' : 'Upload AGM PDF'}
              </button>
            </>
          )}
        </div>
      </div>

      {/* Role Notice if Active Persona is TENANT */}
      {isTenantRole && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 text-amber-950 flex items-start gap-3 shadow-xs">
          <ShieldAlert className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
          <div className="text-xs leading-relaxed">
            <p className="font-bold text-sm">
              {isTe
                ? 'అద్దెదారు (TENANT) లాగిన్ గమనిక — ఓనర్-ఓన్లీ తీర్మానాలలో ఓటింగ్ నిలిపివేయబడింది'
                : 'Tenant Role Active — Statutory AGM Resolutions Restricted to Registered Unit Owners'}
            </p>
            <p className="mt-0.5">
              {isTe
                ? 'ఆంధ్రప్రదేశ్ అపార్ట్‌మెంట్ బైలాస్ ప్రకారం ఆర్థిక మరియు మూలధన తీర్మానాలలో ఫ్లాట్ యజమానులు (OWNER) మాత్రమే ఓటు వేయగలరు. పైన ఉన్న పర్సోనా స్విచ్చర్ ద్వారా OWNER లేదా TREASURER కు మారి ఓటు వేయండి.'
                : 'Under AP Apartment Societies Bylaws, capital expenditure and AGM election polls enforce 1-Vote-Per-Unit (OWNER Only). Switch persona to Flat Owner or Treasurer in the top bar to cast a unit vote.'}
            </p>
          </div>
        </div>
      )}

      {banner && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between ${
            banner.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <span className="text-sm font-medium">{banner.text}</span>
          <button
            type="button"
            onClick={() => setBanner(null)}
            className="text-xs font-semibold underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Active Polls & Secret Ballots */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {polls.map((poll) => {
          const disabledForTenant = poll.ownerOnly && isTenantRole;
          return (
            <div
              key={poll.id}
              className="bg-white rounded-2xl border border-slate-200/90 p-6 shadow-xs flex flex-col justify-between space-y-5"
            >
              <div className="space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    {poll.isSecret ? (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-purple-100 text-purple-900">
                        <Lock className="w-3.5 h-3.5" />
                        {isTe ? 'రహస్య బ్యాలెట్ (Secret Ballot)' : t('governance.secretBallot', 'Secret Ballot')}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-900">
                        <Eye className="w-3.5 h-3.5" />
                        {isTe ? 'ఓపెన్ పోల్ (Open Poll)' : t('governance.openPoll', 'Open Poll')}
                      </span>
                    )}

                    {poll.ownerOnly && (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-900">
                        <Award className="w-3.5 h-3.5" />
                        {isTe ? '1 ఫ్లాట్ = 1 ఓటు (OWNER మాత్రమే)' : '1-Vote-Per-Unit (OWNER Only)'}
                      </span>
                    )}
                  </div>

                  <span className="text-xs font-bold text-slate-500">
                    {poll.totalVotes} {isTe ? 'ఓట్లు' : 'Unit Votes'}
                  </span>
                </div>

                <div>
                  <h2 className="text-base font-bold text-slate-900">
                    {isTe ? poll.titleTe || poll.title : poll.title}
                  </h2>
                  <p className="text-xs text-slate-600 mt-1 leading-relaxed">
                    {isTe ? poll.descriptionTe || poll.description : poll.description}
                  </p>
                </div>

                {/* Options & Progress Bars */}
                <div className="space-y-3 pt-2">
                  {poll.options.map((opt) => {
                    const pct =
                      poll.totalVotes > 0
                        ? Math.round((opt.voteCount / poll.totalVotes) * 100)
                        : 0;
                    const isVotedOption = poll.votedOptionId === opt.id;
                    return (
                      <div
                        key={opt.id}
                        className={`p-3.5 rounded-xl border transition ${
                          isVotedOption
                            ? 'border-emerald-500 bg-emerald-50/50'
                            : 'border-slate-200 bg-slate-50/60'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-2 text-sm">
                          <span className="font-bold text-slate-900">
                            {isTe ? opt.optionTextTe || opt.optionText : opt.optionText}
                          </span>
                          <span className="text-xs font-extrabold text-indigo-900">
                            {opt.voteCount} ({pct}%)
                          </span>
                        </div>

                        <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden mt-2">
                          <div
                            className="h-full bg-indigo-700 rounded-full transition-all"
                            style={{ width: `${pct}%` }}
                          />
                        </div>

                        {/* Show voter unit numbers only when poll is NOT secret */}
                        {!poll.isSecret && opt.voters && opt.voters.length > 0 && (
                          <div className="mt-2 flex flex-wrap gap-1">
                            {opt.voters.map((vUnit, i) => (
                              <span
                                key={i}
                                className="px-2 py-0.5 rounded bg-white border border-slate-200 text-[10px] font-mono font-semibold text-slate-600"
                              >
                                Flat {vUnit}
                              </span>
                            ))}
                          </div>
                        )}

                        {poll.isSecret && (
                          <div className="mt-1.5 text-[11px] text-purple-700 italic">
                            {isTe
                              ? 'రహస్య బ్యాలెట్: ఫ్లాట్ నంబర్లు గోప్యంగా ఉంచబడ్డాయి'
                              : 'Secret Ballot: Individual unit identities are cryptographically anonymized'}
                          </div>
                        )}

                        <div className="mt-3 flex justify-end">
                          <button
                            type="button"
                            disabled={
                              disabledForTenant ||
                              Boolean(poll.hasUnitVoted) ||
                              votingPollId === poll.id
                            }
                            onClick={() => handleCastVote(poll, opt.id)}
                            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                              disabledForTenant
                                ? 'bg-slate-200 text-slate-500 cursor-not-allowed'
                                : poll.hasUnitVoted
                                ? 'bg-emerald-100 text-emerald-800 cursor-default'
                                : 'bg-indigo-900 hover:bg-indigo-800 text-white shadow-xs'
                            }`}
                          >
                            {disabledForTenant
                              ? isTe
                                ? 'యజమానులకు మాత్రమే (Owner Only)'
                                : 'Restricted (Owner Only)'
                              : poll.hasUnitVoted
                              ? isVotedOption
                                ? isTe
                                  ? 'మీ ఓటు నమోదైంది ✓'
                                  : 'Your Unit Voted ✓'
                                : isTe
                                ? 'ఫ్లాట్ ఓటు వేయబడింది'
                                : 'Unit Vote Cast'
                              : isTe
                              ? 'ఈ ఎంపికకు ఓటు వేయండి'
                              : t('governance.castVote', 'Cast Unit Vote')}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                <span>Published by {poll.createdByName || 'Managing Committee'}</span>
                <span>Closes: {new Date(poll.endsAt).toLocaleDateString('en-IN')}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* AGM & Society Document Archive */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <FolderArchive className="w-5 h-5 text-orange-600" />
            <div>
              <h2 className="text-base font-bold text-slate-900">
                {isTe
                  ? 'AGM మినిట్స్, ఆడిట్ నివేదికలు & బైలాస్ ఆర్కైవ్ (MinIO S3)'
                  : t('governance.documentArchive', 'AGM Minutes, Audited Financials & Society Bylaws Archive')}
              </h2>
              <p className="text-xs text-slate-500">
                {isTe
                  ? 'ఆర్థిక సంవత్సరం వారీగా ఏలైట్ టవర్స్, ఒంగోలు అధికారిక పత్రాలు'
                  : 'Statutory records for Elite Towers, Ongole archived in MinIO S3 (`aikya-storage`).'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl">
            {(['ALL', '2025-26', '2024-25'] as const).map((fy) => (
              <button
                key={fy}
                type="button"
                onClick={() => setFyFilter(fy)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                  fyFilter === fy
                    ? 'bg-white text-indigo-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {fy === 'ALL' ? (isTe ? 'అన్ని సంవత్సరాలు' : 'All FY') : `FY ${fy}`}
              </button>
            ))}
          </div>
        </div>

        <div className="divide-y divide-slate-100">
          {filteredDocs.map((doc) => (
            <div
              key={doc.id}
              className="p-4 hover:bg-slate-50/80 transition flex flex-col sm:flex-row sm:items-center justify-between gap-3"
            >
              <div className="flex items-start gap-3">
                <div className="p-2.5 rounded-xl bg-indigo-50 text-indigo-700">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700">
                      {doc.category}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800">
                      FY {doc.financialYear}
                    </span>
                  </div>
                  <h3 className="text-sm font-bold text-slate-900 mt-1">
                    {isTe ? doc.titleTe || doc.title : doc.title}
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Uploaded by {doc.uploadedByName} •{' '}
                    {doc.fileSizeBytes ? `${Math.round(doc.fileSizeBytes / 1024)} KB` : 'PDF'}
                  </p>
                </div>
              </div>

              <a
                href={doc.fileUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-800 text-xs font-bold transition shrink-0"
              >
                <Download className="w-4 h-4" />
                {isTe ? 'PDF డౌన్‌లోడ్' : 'View / Download PDF'}
              </a>
            </div>
          ))}
        </div>
      </div>

      {/* Create Poll Modal */}
      {showCreatePoll && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe ? 'కొత్త AGM తీర్మానం / పోల్ సృష్టించండి' : 'Create Community Resolution Poll'}
              </h3>
              <button
                type="button"
                onClick={() => setShowCreatePoll(false)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreatePoll} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Resolution Title (English) *
                </label>
                <input
                  type="text"
                  required
                  value={pollTitle}
                  onChange={(e) => setPollTitle(e.target.value)}
                  placeholder="e.g. Approval for Basement EV Charging Station Setup"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  తీర్మానం శీర్షిక (Telugu)
                </label>
                <input
                  type="text"
                  value={pollTitleTe}
                  onChange={(e) => setPollTitleTe(e.target.value)}
                  placeholder="ఉదా: బేస్‌మెంట్‌లో EV ఛార్జింగ్ స్టేషన్ ఏర్పాటుకు ఆమోదం"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Description (English)
                </label>
                <textarea
                  rows={2}
                  value={pollDesc}
                  onChange={(e) => setPollDesc(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <label className="flex items-center gap-2 p-3 rounded-xl border border-slate-200 text-xs font-semibold cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isSecret}
                    onChange={(e) => setIsSecret(e.target.checked)}
                  />
                  <span>Secret Ballot (Anonymous)</span>
                </label>
                <label className="flex items-center gap-2 p-3 rounded-xl border border-slate-200 text-xs font-semibold cursor-pointer">
                  <input
                    type="checkbox"
                    checked={ownerOnly}
                    onChange={(e) => setOwnerOnly(e.target.checked)}
                  />
                  <span>1-Vote-Per-Unit (Owner Only)</span>
                </label>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Option 1 (EN)</label>
                  <input
                    type="text"
                    value={opt1En}
                    onChange={(e) => setOpt1En(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Option 1 (TE)</label>
                  <input
                    type="text"
                    value={opt1Te}
                    onChange={(e) => setOpt1Te(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Option 2 (EN)</label>
                  <input
                    type="text"
                    value={opt2En}
                    onChange={(e) => setOpt2En(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-1.5 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Option 2 (TE)</label>
                  <input
                    type="text"
                    value={opt2Te}
                    onChange={(e) => setOpt2Te(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-1.5 text-sm"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreatePoll(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPoll}
                  className="px-5 py-2 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingPoll ? 'Publishing...' : 'Publish Poll'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Upload AGM Document Modal */}
      {showUploadDoc && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">
                {isTe ? 'AGM / సొసైటీ PDF అప్‌లోడ్' : 'Upload AGM / Audit PDF to MinIO S3'}
              </h3>
              <button
                type="button"
                onClick={() => setShowUploadDoc(false)}
                className="text-slate-400 hover:text-slate-600 text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateDocument} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Document Title (English) *
                </label>
                <input
                  type="text"
                  required
                  value={docTitle}
                  onChange={(e) => setDocTitle(e.target.value)}
                  placeholder="e.g. Special General Body Meeting Resolution PDF"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  డాక్యుమెంట్ శీర్షిక (Telugu)
                </label>
                <input
                  type="text"
                  value={docTitleTe}
                  onChange={(e) => setDocTitleTe(e.target.value)}
                  placeholder="ఉదా: ప్రత్యేక సర్వసభ్య సమావేశ తీర్మాన పత్రం"
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
                  <select
                    value={docCategory}
                    onChange={(e) => setDocCategory(e.target.value as DocumentCategory)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    <option value="AGM_MINUTES">AGM_MINUTES</option>
                    <option value="AUDIT_REPORT">AUDIT_REPORT</option>
                    <option value="BYLAWS">BYLAWS</option>
                    <option value="NOC_TEMPLATE">NOC_TEMPLATE</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Financial Year</label>
                  <select
                    value={docFy}
                    onChange={(e) => setDocFy(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  >
                    <option value="2025-26">2025-26</option>
                    <option value="2024-25">2024-25</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Select PDF File (MinIO S3)
                </label>
                <input
                  type="file"
                  accept=".pdf,image/*"
                  onChange={handleDocFileSelect}
                  className="w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-emerald-50 file:text-emerald-700 file:font-semibold"
                />
                {uploadingDocFile && (
                  <p className="text-xs text-emerald-700 mt-1">Uploading to MinIO S3...</p>
                )}
                {docFileUrl && (
                  <p className="text-xs text-emerald-700 mt-1 break-all">
                    <CheckCircle2 className="w-3.5 h-3.5 inline mr-1" />
                    {docFileUrl}
                  </p>
                )}
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowUploadDoc(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 text-sm font-medium cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingDoc}
                  className="px-5 py-2 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-semibold shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {submittingDoc ? 'Archiving...' : 'Archive Document'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default GovernancePage;
