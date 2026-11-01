import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Phone, KeyRound, CheckCircle2, Sparkles, ShieldCheck } from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { useAppStore, DEMO_PERSONAS, DemoPersonaKey } from '../../store/useAppStore';
import { apiClient } from '../../lib/apiClient';

const QUICK_PHONES: Array<{
  phone: string;
  personaKey: DemoPersonaKey;
  name: string;
  role: string;
  flat: string;
}> = [
  {
    phone: '+919848011111',
    personaKey: 'treasurer',
    name: 'Ramesh Naidu',
    role: 'TREASURER (Elite Towers)',
    flat: 'A-101',
  },
  {
    phone: '+919848022222',
    personaKey: 'owner',
    name: 'Lakshmi Devi',
    role: 'OWNER (Elite Towers)',
    flat: 'A-102',
  },
  {
    phone: '+919848033333',
    personaKey: 'tenant',
    name: 'Karthikeya Sharma',
    role: 'TENANT (Elite Towers)',
    flat: 'B-201',
  },
  {
    phone: '+919848044444',
    personaKey: 'assoc2',
    name: 'Srinivas Rao',
    role: 'PRESIDENT (Cyber Meadows)',
    flat: 'C-301',
  },
];

export const LoginModal: React.FC = () => {
  const { t } = useTranslation();
  const { loginModalOpen, setLoginModalOpen, setAuth, switchPersona } = useAppStore();

  const [phoneNumber, setPhoneNumber] = useState('+919848011111');
  const [otpCode, setOtpCode] = useState('123456');
  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [mockHint, setMockHint] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.sendOtp({ phoneNumber });
      setMockHint(res.mockOtpHint || '123456');
      setOtpCode(res.mockOtpHint || '123456');
      setStep('otp');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      // Check if phone matches one of our seeded demo personas for full rich state
      const matched = QUICK_PHONES.find((p) => p.phone === phoneNumber.trim());
      const res = await apiClient.verifyOtp({ phoneNumber, otpCode });
      if (matched && (!res.memberships || res.memberships.length === 0)) {
        switchPersona(matched.personaKey);
      } else {
        setAuth(res.accessToken, res.user, res.memberships);
      }
      setStep('phone');
      setLoginModalOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickPersonaSelect = (item: (typeof QUICK_PHONES)[number]) => {
    setPhoneNumber(item.phone);
    const persona = DEMO_PERSONAS[item.personaKey];
    if (persona) {
      switchPersona(item.personaKey);
      setLoginModalOpen(false);
    }
  };

  return (
    <Modal
      isOpen={loginModalOpen}
      onClose={() => setLoginModalOpen(false)}
      title={t('auth.loginTitle', 'Passwordless Phone + OTP Sign In')}
      subtitle={t(
        'auth.loginSubtitle',
        'Enter your registered mobile number for Elite Towers, Ongole or Cyber Meadows RWA'
      )}
      size="md"
    >
      <div className="space-y-5">
        {/* Quick 1-Click Seeded Accounts */}
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2.5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-orange-500" />
              {t('auth.quickDemoAccounts', '1-Click Demo Personas (Instant JWT)')}
            </span>
            <Badge variant="saffron" size="xs">
              Mock OTP: 123456
            </Badge>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {QUICK_PHONES.map((item) => (
              <button
                key={item.phone}
                type="button"
                onClick={() => handleQuickPersonaSelect(item)}
                className="text-left p-2.5 rounded-xl border border-slate-200 bg-white hover:border-indigo-400 hover:bg-indigo-50/40 transition-all cursor-pointer"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-900">
                    {item.name}
                  </span>
                  <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-slate-100 text-slate-700">
                    {item.flat}
                  </span>
                </div>
                <p className="text-[11px] text-indigo-600 font-medium mt-0.5">
                  {item.role}
                </p>
                <p className="text-[10px] text-slate-400 font-mono mt-0.5">
                  {item.phone}
                </p>
              </button>
            ))}
          </div>
        </div>

        {/* Interactive Phone / OTP Form */}
        {step === 'phone' ? (
          <form onSubmit={handleSendOtp} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                {t('auth.phoneNumber', 'Mobile Number (India +91)')}
              </label>
              <div className="relative">
                <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="+919848011111"
                  required
                  className="w-full pl-10 pr-4 py-2.5 text-sm rounded-xl border border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
                />
              </div>
            </div>

            {error && (
              <p className="text-xs text-rose-600 bg-rose-50 px-3 py-2 rounded-lg">
                {error}
              </p>
            )}

            <div className="flex items-center justify-end gap-2 pt-1">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setLoginModalOpen(false)}
              >
                {t('common.cancel', 'Cancel')}
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={loading}
                leftIcon={<KeyRound className="w-4 h-4" />}
              >
                {t('auth.sendOtp', 'Send OTP')}
              </Button>
            </div>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-2.5 text-xs text-emerald-800">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <div>
                <span>
                  OTP sent to <strong>{phoneNumber}</strong>.
                </span>{' '}
                {mockHint && (
                  <span className="font-mono font-bold underline">
                    Demo OTP: {mockHint}
                  </span>
                )}
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                {t('auth.enterOtp', '6-Digit Verification OTP')}
              </label>
              <input
                type="text"
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value)}
                maxLength={6}
                required
                className="w-full px-4 py-2.5 text-center tracking-[0.4em] font-mono text-base font-bold rounded-xl border border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 focus:outline-none"
              />
            </div>

            {error && (
              <p className="text-xs text-rose-600 bg-rose-50 px-3 py-2 rounded-lg">
                {error}
              </p>
            )}

            <div className="flex items-center justify-between pt-1">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setStep('phone')}
              >
                ← {t('auth.changePhone', 'Change Number')}
              </Button>
              <Button
                type="submit"
                variant="emerald"
                size="md"
                isLoading={loading}
                leftIcon={<ShieldCheck className="w-4 h-4" />}
              >
                {t('auth.verifyAndLogin', 'Verify & Sign In')}
              </Button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  );
};

export default LoginModal;
