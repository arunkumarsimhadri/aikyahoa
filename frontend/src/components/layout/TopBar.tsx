import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  Languages,
  Building2,
  UserCheck,
  KeyRound,
  Sparkles,
  Shield,
} from 'lucide-react';
import {
  useAppStore,
  DEMO_PERSONAS,
  DemoPersonaKey,
} from '../../store/useAppStore';
import { apiClient } from '../../lib/apiClient';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';

export const TopBar: React.FC = () => {
  const { t } = useTranslation();
  const {
    locale,
    setLocale,
    user,
    activeAssociation,
    activePersona,
    switchPersona,
    setLoginModalOpen,
  } = useAppStore();

  const handleToggleLanguage = async () => {
    const nextLocale = locale === 'en' ? 'te' : 'en';
    setLocale(nextLocale);
    try {
      await apiClient.updateLocale({ locale: nextLocale });
    } catch {
      // Offline fallback handled gracefully
    }
  };

  const handleAssociationChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const schema = e.target.value;
    if (schema === 'aikya_assoc_2') {
      switchPersona('assoc2');
    } else if (activePersona === 'assoc2') {
      switchPersona('treasurer');
    }
  };

  const handlePersonaChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const key = e.target.value as DemoPersonaKey;
    switchPersona(key);
  };

  return (
    <header className="sticky top-0 z-30 bg-white/90 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 py-2.5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Left: Mobile Brand + Active Association Context */}
        <div className="flex items-center gap-3 min-w-0">
          <div className="md:hidden w-9 h-9 rounded-xl bg-gradient-to-br from-orange-500 to-indigo-600 flex items-center justify-center text-white font-bold shadow-xs shrink-0">
            ఐ
          </div>

          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
                {activeAssociation?.schemaName === 'aikya_assoc_1'
                  ? locale === 'te'
                    ? 'ఏలైట్ టవర్స్ — ఒంగోలు (Elite Towers)'
                    : 'Elite Towers — Ongole, AP (523001)'
                  : activeAssociation?.associationName || 'Elite Towers'}
              </h1>
              <Badge variant="indigo" size="xs" className="hidden sm:inline-flex">
                {activeAssociation?.associationCode || 'ELITE_TOWERS_ONGOLE'}
              </Badge>
            </div>
            <p className="text-xs text-slate-500 truncate">
              {user?.fullName} •{' '}
              <span className="font-medium text-slate-700">
                {t('common.flat', 'Flat')} {activeAssociation?.unitNumber || 'A-101'}
              </span>{' '}
              ({t(`roles.${activeAssociation?.role || 'TREASURER'}`)})
            </p>
          </div>
        </div>

        {/* Right: Interactive Switchers (Multi-Tenant, Role Persona, Bilingual EN/TE, OTP Login) */}
        <div className="flex flex-wrap items-center gap-2 sm:gap-2.5">
          {/* Association / Multi-Tenant Schema Switcher */}
          <div className="relative flex items-center bg-slate-100/90 hover:bg-slate-100 rounded-xl px-2.5 py-1.5 border border-slate-200/80 text-xs">
            <Building2 className="w-3.5 h-3.5 text-indigo-600 mr-1.5 shrink-0" />
            <label htmlFor="tenant-switcher" className="sr-only">
              Select Association Tenant
            </label>
            <select
              id="tenant-switcher"
              value={activeAssociation?.schemaName || 'aikya_assoc_1'}
              onChange={handleAssociationChange}
              className="bg-transparent font-medium text-slate-700 focus:outline-none cursor-pointer pr-1 max-w-[190px] sm:max-w-[240px] truncate"
              title="Switch PostgreSQL Multi-Tenant Schema (X-Tenant-ID)"
            >
              <option value="aikya_assoc_1">
                {locale === 'te'
                  ? 'ఏలైట్ టవర్స్, ఒంగోలు (aikya_assoc_1)'
                  : 'Elite Towers, Ongole (aikya_assoc_1)'}
              </option>
              <option value="aikya_assoc_2">
                {locale === 'te'
                  ? 'సైబర్ మెడోస్ RWA (aikya_assoc_2)'
                  : 'Cyber Meadows RWA (aikya_assoc_2)'}
              </option>
            </select>
          </div>

          {/* Demo Persona / Role Switcher */}
          <div className="relative flex items-center bg-orange-50/90 hover:bg-orange-100/70 rounded-xl px-2.5 py-1.5 border border-orange-200/80 text-xs">
            <UserCheck className="w-3.5 h-3.5 text-orange-600 mr-1.5 shrink-0" />
            <label htmlFor="persona-switcher" className="sr-only">
              Switch Demo Role Persona
            </label>
            <select
              id="persona-switcher"
              value={activePersona}
              onChange={handlePersonaChange}
              className="bg-transparent font-semibold text-orange-900 focus:outline-none cursor-pointer pr-1 max-w-[180px] sm:max-w-[230px] truncate"
              title="1-Click RBAC & Persona Switcher"
            >
              {(Object.keys(DEMO_PERSONAS) as DemoPersonaKey[]).map((key) => {
                const persona = DEMO_PERSONAS[key];
                return (
                  <option key={key} value={key}>
                    {locale === 'te' ? persona.labelTe : persona.labelEn}
                  </option>
                );
              })}
            </select>
          </div>

          {/* Bilingual Language Toggle (English <-> తెలుగు) */}
          <button
            type="button"
            onClick={handleToggleLanguage}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border border-indigo-200 bg-indigo-50/80 hover:bg-indigo-100 text-indigo-800 transition-colors cursor-pointer shadow-2xs"
            title="Switch Language (English / తెలుగు)"
          >
            <Languages className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
            <span>{locale === 'en' ? 'తెలుగు (TE)' : 'English (EN)'}</span>
            <Sparkles className="w-3 h-3 text-orange-500 shrink-0" />
          </button>

          {/* Phone + OTP Login Modal Trigger */}
          <Button
            variant="outline"
            size="sm"
            onClick={() => setLoginModalOpen(true)}
            leftIcon={<KeyRound className="w-3.5 h-3.5 text-indigo-600" />}
            className="text-xs py-1.5 px-2.5 rounded-xl"
          >
            <span className="hidden lg:inline">
              {t('auth.otpLogin', 'Phone OTP Login')}
            </span>
            <span className="lg:hidden">OTP</span>
          </Button>

          {/* Active Role Badge */}
          <div className="hidden xl:flex items-center">
            <Badge
              variant={
                activeAssociation?.role === 'TREASURER' ||
                activeAssociation?.role === 'PRESIDENT'
                  ? 'saffron'
                  : activeAssociation?.role === 'OWNER'
                  ? 'emerald'
                  : 'info'
              }
              size="sm"
              icon={<Shield className="w-3 h-3" />}
            >
              {t(`roles.${activeAssociation?.role || 'TREASURER'}`)}
            </Badge>
          </div>
        </div>
      </div>
    </header>
  );
};

export default TopBar;
