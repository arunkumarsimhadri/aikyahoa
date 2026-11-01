import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Calendar,
  Clock,
  Users,
  IndianRupee,
  CheckCircle2,
  Lock,
  Sparkles,
  AlertCircle,
  Building2,
} from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { apiClient } from '../lib/apiClient';
import type { AmenityDto, AmenitySlotDto, AmenityBookingDto } from '../types/api';

function unwrap<T>(res: unknown): T {
  if (res && typeof res === 'object' && 'data' in (res as Record<string, unknown>)) {
    return (res as { data: T }).data;
  }
  return res as T;
}

const DEFAULT_TIME_SLOTS: { startTime: string; endTime: string }[] = [
  { startTime: '06:00', endTime: '08:00' },
  { startTime: '08:00', endTime: '10:00' },
  { startTime: '10:00', endTime: '12:00' },
  { startTime: '12:00', endTime: '14:00' },
  { startTime: '14:00', endTime: '16:00' },
  { startTime: '16:00', endTime: '18:00' },
  { startTime: '18:00', endTime: '20:00' },
  { startTime: '20:00', endTime: '22:00' },
];

export const AmenitiesPage: React.FC = () => {
  const { t } = useTranslation();
  const { locale, activeAssociation } = useAppStore();
  const isTe = locale === 'te';

  const [amenities, setAmenities] = useState<AmenityDto[]>([]);
  const [selectedAmenity, setSelectedAmenity] = useState<AmenityDto | null>(null);
  const [bookingDate, setBookingDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [slots, setSlots] = useState<AmenitySlotDto[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<AmenitySlotDto | null>(null);
  const [bookings, setBookings] = useState<AmenityBookingDto[]>([]);
  const [purpose, setPurpose] = useState<string>('Family Gathering / Satyanarayana Swamy Vratam');
  const [guestCount, setGuestCount] = useState<number>(25);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [banner, setBanner] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const loadAmenitiesAndBookings = useCallback(async () => {
    try {
      const [amRes, bkRes] = await Promise.all([
        apiClient.getAmenities(),
        apiClient.getAmenityBookings(),
      ]);
      const amList = unwrap<AmenityDto[]>(amRes) || [];
      setAmenities(amList);
      setBookings(unwrap<AmenityBookingDto[]>(bkRes) || []);
      if (amList.length > 0 && !selectedAmenity) {
        setSelectedAmenity(amList[0]);
      }
    } catch (err: any) {
      setBanner({ type: 'error', text: err?.message || 'Failed to load amenities' });
    }
  }, [activeAssociation?.schemaName, selectedAmenity]);

  const loadSlots = useCallback(async () => {
    if (!selectedAmenity) return;
    try {
      const res = await apiClient.getAmenitySlots(selectedAmenity.id, bookingDate);
      const apiSlots = unwrap<AmenitySlotDto[]>(res) || [];
      if (apiSlots.length > 0) {
        // Ensure full 06:00 to 22:00 2-hour grid is rendered, merging live conflict data
        const merged: AmenitySlotDto[] = DEFAULT_TIME_SLOTS.map((base) => {
          const found = apiSlots.find(
            (s) =>
              s.startTime.startsWith(base.startTime) && s.endTime.startsWith(base.endTime)
          );
          if (found) return found;
          const bookingConflict = bookings.find(
            (b) =>
              b.amenityId === selectedAmenity.id &&
              b.bookingDate === bookingDate &&
              b.status === 'CONFIRMED' &&
              base.startTime < b.endTime.slice(0, 5) &&
              base.endTime > b.startTime.slice(0, 5)
          );
          return {
            startTime: base.startTime,
            endTime: base.endTime,
            available: !bookingConflict,
            bookedByUnit: bookingConflict?.unitNumber,
            bookingId: bookingConflict?.id,
          };
        });
        setSlots(merged);
      } else {
        setSlots(
          DEFAULT_TIME_SLOTS.map((s) => ({
            ...s,
            available: true,
          }))
        );
      }
    } catch {
      setSlots(
        DEFAULT_TIME_SLOTS.map((s) => ({
          ...s,
          available: true,
        }))
      );
    }
  }, [selectedAmenity, bookingDate, bookings]);

  useEffect(() => {
    loadAmenitiesAndBookings();
  }, [loadAmenitiesAndBookings]);

  useEffect(() => {
    loadSlots();
    setSelectedSlot(null);
  }, [loadSlots]);

  const handleConfirmBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAmenity || !selectedSlot) return;
    setSubmitting(true);
    setBanner(null);
    try {
      const res = await apiClient.createAmenityBooking({
        amenityId: selectedAmenity.id,
        unitId: 1,
        unitNumber: activeAssociation?.unitNumber || 'A-101',
        bookingDate,
        startTime: selectedSlot.startTime,
        endTime: selectedSlot.endTime,
        purpose,
        guestCount,
      });
      const booked = unwrap<AmenityBookingDto>(res);
      setBanner({
        type: 'success',
        text: isTe
          ? `${selectedAmenity.nameTe || selectedAmenity.name} స్లాట్ (${selectedSlot.startTime} - ${selectedSlot.endTime}) ఫ్లాట్ ${booked?.unitNumber || activeAssociation?.unitNumber || 'A-101'} కొరకు బుక్ చేయబడింది!`
          : `Confirmed booking for ${selectedAmenity.name} (${selectedSlot.startTime}–${selectedSlot.endTime}) on ${bookingDate}!`,
      });
      setSelectedSlot(null);
      await loadAmenitiesAndBookings();
      await loadSlots();
    } catch (err: any) {
      setBanner({
        type: 'error',
        text:
          err?.message ||
          (isTe
            ? 'ఈ టైమ్ స్లాట్ ఇప్పటికే వేరొక ఫ్లాట్ చేత బుక్ చేయబడింది (Slot Conflict)!'
            : 'Slot Conflict Protected: This time slot is already booked by another resident.'),
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
        <div>
          <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            <Calendar className="w-6 h-6 text-indigo-700" />
            {isTe
              ? 'సౌకర్యాల బుకింగ్ — క్లబ్ హౌస్, పార్టీ హాల్ & స్విమ్మింగ్ పూల్'
              : t('nav.amenities', 'Amenity Slot Booking & Conflict Protection')}
          </h1>
          <p className="text-sm text-slate-600 mt-0.5">
            {isTe
              ? 'ఏలైట్ టవర్స్, ఒంగోలు — 2-గంటల టైమ్ స్లాట్ గ్రిడ్ మరియు రియల్-టైమ్ డబుల్ బుకింగ్ రక్షణ'
              : 'Interactive 2-hour slot reservation grid (06:00 to 22:00) with real-time PostgreSQL overlap conflict prevention.'}
          </p>
        </div>
      </div>

      {banner && (
        <div
          className={`p-4 rounded-xl border flex items-center justify-between ${
            banner.type === 'success'
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-rose-50 border-rose-200 text-rose-900'
          }`}
        >
          <div className="flex items-center gap-2 text-sm font-medium">
            {banner.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            )}
            <span>{banner.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setBanner(null)}
            className="text-xs font-semibold underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* 4 Amenity Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {amenities.map((am) => {
          const isSelected = selectedAmenity?.id === am.id;
          return (
            <button
              key={am.id}
              type="button"
              onClick={() => setSelectedAmenity(am)}
              className={`text-left p-5 rounded-2xl border transition cursor-pointer flex flex-col justify-between gap-3 ${
                isSelected
                  ? 'bg-indigo-950 text-white border-indigo-900 shadow-md ring-2 ring-orange-400'
                  : 'bg-white text-slate-900 border-slate-200 hover:border-indigo-300 shadow-xs'
              }`}
            >
              <div>
                <div className="flex items-center justify-between gap-2">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                      isSelected ? 'bg-orange-500 text-white' : 'bg-indigo-50 text-indigo-800'
                    }`}
                  >
                    {am.code}
                  </span>
                  <span
                    className={`text-xs font-bold flex items-center ${
                      isSelected ? 'text-amber-300' : 'text-emerald-700'
                    }`}
                  >
                    {am.hourlyRate > 0 ? (
                      <>
                        <IndianRupee className="w-3.5 h-3.5" />
                        {am.hourlyRate}/hr
                      </>
                    ) : isTe ? (
                      'ఉచితం (Free)'
                    ) : (
                      'Free for Residents'
                    )}
                  </span>
                </div>
                <h3 className="text-base font-bold mt-2.5">
                  {isTe ? am.nameTe || am.name : am.name}
                </h3>
                <p
                  className={`text-xs mt-1 line-clamp-2 ${
                    isSelected ? 'text-indigo-200' : 'text-slate-500'
                  }`}
                >
                  {isTe ? am.descriptionTe || am.description : am.description}
                </p>
              </div>

              <div
                className={`pt-3 border-t flex items-center justify-between text-xs ${
                  isSelected ? 'border-white/15 text-indigo-200' : 'border-slate-100 text-slate-500'
                }`}
              >
                <span className="inline-flex items-center gap-1">
                  <Users className="w-3.5 h-3.5" /> {am.capacity} pax
                </span>
                <span className="inline-flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" /> {am.openTime?.slice(0, 5)}–{am.closeTime?.slice(0, 5)}
                </span>
              </div>
            </button>
          );
        })}
      </div>

      {/* Interactive Time-Slot Grid + Booking Form */}
      {selectedAmenity && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Slot Grid (7 cols) */}
          <div className="lg:col-span-7 bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  {isTe
                    ? `${selectedAmenity.nameTe || selectedAmenity.name} — 2-గంటల టైమ్ స్లాట్లు`
                    : `${selectedAmenity.name} — Interactive 2-Hour Slot Grid`}
                </h2>
                <p className="text-xs text-slate-500">
                  {isTe
                    ? 'ఆకుపచ్చ స్లాట్లు అందుబాటులో ఉన్నాయి; ఎరుపు స్లాట్లు ఇప్పటికే బుక్ చేయబడ్డాయి'
                    : 'Green slots are available to book; Red/Amber slots show active flat reservations.'}
                </p>
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase text-slate-500 mb-1">
                  {isTe ? 'బుకింగ్ తేదీ' : 'Select Date'}
                </label>
                <input
                  type="date"
                  value={bookingDate}
                  onChange={(e) => setBookingDate(e.target.value)}
                  className="rounded-xl border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-800"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {slots.map((slot) => {
                const startLabel = slot.startTime.slice(0, 5);
                const endLabel = slot.endTime.slice(0, 5);
                const isChosen =
                  selectedSlot?.startTime === slot.startTime &&
                  selectedSlot?.endTime === slot.endTime;

                if (!slot.available) {
                  return (
                    <div
                      key={`${startLabel}-${endLabel}`}
                      className="p-4 rounded-xl border border-rose-200 bg-rose-50/80 text-rose-900 flex items-center justify-between opacity-90 cursor-not-allowed"
                    >
                      <div>
                        <div className="font-bold text-sm flex items-center gap-1.5">
                          <Clock className="w-4 h-4 text-rose-600" />
                          {startLabel} – {endLabel}
                        </div>
                        <div className="text-xs font-semibold text-rose-700 mt-1">
                          {isTe
                            ? `ఫ్లాట్ ${slot.bookedByUnit || 'A-101'} చే బుక్ చేయబడింది`
                            : `Booked by Flat ${slot.bookedByUnit || 'A-101'}`}
                        </div>
                      </div>
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-rose-200/80 text-rose-900">
                        <Lock className="w-3 h-3" />
                        {isTe ? 'బుక్ అయింది' : 'BOOKED'}
                      </span>
                    </div>
                  );
                }

                return (
                  <button
                    key={`${startLabel}-${endLabel}`}
                    type="button"
                    onClick={() => setSelectedSlot(slot)}
                    className={`p-4 rounded-xl border text-left transition cursor-pointer flex items-center justify-between ${
                      isChosen
                        ? 'border-emerald-600 bg-emerald-600 text-white shadow-md ring-2 ring-emerald-300'
                        : 'border-emerald-200 bg-emerald-50/60 hover:bg-emerald-100/70 text-emerald-950'
                    }`}
                  >
                    <div>
                      <div className="font-bold text-sm flex items-center gap-1.5">
                        <Clock className={`w-4 h-4 ${isChosen ? 'text-white' : 'text-emerald-600'}`} />
                        {startLabel} – {endLabel}
                      </div>
                      <div
                        className={`text-xs mt-1 ${
                          isChosen ? 'text-emerald-100' : 'text-emerald-700 font-medium'
                        }`}
                      >
                        {isChosen
                          ? isTe
                            ? 'ఎంచుకోబడింది ✓'
                            : 'Selected for Booking ✓'
                          : isTe
                          ? 'బుకింగ్ కొరకు అందుబాటులో ఉంది'
                          : 'Available — Click to Select'}
                      </div>
                    </div>
                    <span
                      className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                        isChosen
                          ? 'bg-white text-emerald-800'
                          : 'bg-emerald-200/70 text-emerald-900'
                      }`}
                    >
                      {isTe ? 'అందుబాటులో' : 'AVAILABLE'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Booking Confirmation Form (5 cols) */}
          <div className="lg:col-span-5 bg-white rounded-2xl border border-slate-200/80 p-6 shadow-sm flex flex-col justify-between space-y-4">
            <form onSubmit={handleConfirmBooking} className="space-y-4">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-orange-500" />
                <h3 className="text-base font-bold text-slate-900">
                  {isTe ? 'స్లాట్ బుకింగ్ నిర్ధారణ' : 'Confirm Amenity Slot Reservation'}
                </h3>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-500">{isTe ? 'సౌకర్యం:' : 'Amenity:'}</span>
                  <span className="font-bold text-slate-900">
                    {isTe ? selectedAmenity.nameTe || selectedAmenity.name : selectedAmenity.name}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">{isTe ? 'ఫ్లాట్ నంబర్:' : 'Resident Unit:'}</span>
                  <span className="font-bold text-indigo-800">
                    Flat {activeAssociation?.unitNumber || 'A-101'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">{isTe ? 'తేదీ & సమయం:' : 'Date & Slot:'}</span>
                  <span className="font-bold text-emerald-700">
                    {selectedSlot
                      ? `${bookingDate} (${selectedSlot.startTime.slice(0, 5)} – ${selectedSlot.endTime.slice(0, 5)})`
                      : isTe
                      ? 'ఎడమవైపు గ్రిడ్ నుండి స్లాట్ ఎంచుకోండి'
                      : 'Select an available green slot'}
                  </span>
                </div>
                <div className="flex justify-between pt-1 border-t border-slate-200">
                  <span className="text-slate-500">{isTe ? 'బుకింగ్ రుసుము (2 గంటలు):' : 'Estimated Fee (2 hrs):'}</span>
                  <span className="font-extrabold text-slate-900">
                    ₹{(selectedAmenity.hourlyRate || 0) * 2}
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'కార్యక్రమం / బుకింగ్ ఉద్దేశ్యం' : 'Event / Booking Purpose'}
                </label>
                <input
                  type="text"
                  required
                  value={purpose}
                  onChange={(e) => setPurpose(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  {isTe ? 'అతిథుల సంఖ్య (Expected Guests)' : 'Expected Guest Count'}
                </label>
                <input
                  type="number"
                  min={1}
                  max={selectedAmenity.capacity}
                  value={guestCount}
                  onChange={(e) => setGuestCount(Number(e.target.value))}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                />
              </div>

              <button
                type="submit"
                disabled={!selectedSlot || submitting}
                className="w-full py-2.5 px-4 rounded-xl bg-indigo-900 hover:bg-indigo-800 text-white font-semibold text-sm shadow transition cursor-pointer disabled:opacity-50"
              >
                {submitting
                  ? 'Reserving Slot...'
                  : isTe
                  ? 'స్లాట్ బుకింగ్ నిర్ధారించండి'
                  : t('amenities.bookSlot', 'Confirm Slot Booking')}
              </button>
            </form>

            {/* Recent Confirmed Bookings */}
            <div className="pt-4 border-t border-slate-100 space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                {isTe ? 'ఇటీవలి బుకింగ్‌లు' : 'Upcoming Confirmed Reservations'}
              </h4>
              <div className="space-y-2 max-h-44 overflow-y-auto">
                {bookings.slice(0, 5).map((bk) => (
                  <div
                    key={bk.id}
                    className="p-2.5 rounded-xl bg-slate-50 border border-slate-200/80 flex items-center justify-between text-xs"
                  >
                    <div>
                      <span className="font-bold text-slate-900">
                        {isTe ? bk.amenityNameTe || bk.amenityName : bk.amenityName}
                      </span>
                      <div className="text-slate-500">
                        {bk.bookingDate} • {bk.startTime.slice(0, 5)}–{bk.endTime.slice(0, 5)}
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-800 font-bold">
                      <Building2 className="w-3 h-3 inline mr-1" />
                      Flat {bk.unitNumber}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AmenitiesPage;
