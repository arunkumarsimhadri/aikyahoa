import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard,
  ReceiptIndianRupee,
  CheckCircle2,
  Wrench,
  CalendarClock,
  Vote,
  Megaphone,
  ChevronLeft,
  ChevronRight,
  Building2,
  ShieldCheck,
  MapPin,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAppStore, AppTab } from '../../store/useAppStore';
import { Badge } from '../ui/Badge';

interface NavItemConfig {
  id: AppTab;
  labelKey: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
  treasurerHighlight?: boolean;
}

const NAV_ITEMS: NavItemConfig[] = [
  { id: 'dashboard', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { id: 'billing', labelKey: 'nav.billing', icon: ReceiptIndianRupee },
  {
    id: 'reconciliation',
    labelKey: 'nav.reconciliation',
    icon: CheckCircle2,
    treasurerHighlight: true,
  },
  { id: 'helpdesk', labelKey: 'nav.helpdesk', icon: Wrench },
  { id: 'amenities', labelKey: 'nav.amenities', icon: CalendarClock },
  { id: 'governance', labelKey: 'nav.governance', icon: Vote },
  { id: 'notices', labelKey: 'nav.notices', icon: Megaphone },
];

export const Sidebar: React.FC = () => {
  const { t } = useTranslation();
  const {
    activeTab,
    setActiveTab,
    sidebarCollapsed,
    toggleSidebar,
    activeAssociation,
    locale,
  } = useAppStore();

  const roleKey = activeAssociation?.role || 'OWNER';

  return (
    <aside
      className={clsx(
        'hidden md:flex flex-col bg-slate-900 text-slate-100 border-r border-slate-800/80 transition-all duration-200 shrink-0 select-none',
        sidebarCollapsed ? 'w-20' : 'w-72'
      )}
    >
      {/* Brand Header */}
      <div className="h-16 px-4 flex items-center justify-between border-b border-slate-800/80">
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 via-amber-500 to-indigo-600 flex items-center justify-center shadow-md shrink-0 text-white font-bold text-lg">
            ఐ
          </div>
          {!sidebarCollapsed && (
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-base tracking-tight text-white truncate">
                  Aikya
                </span>
                <span className="text-orange-400 font-semibold text-base font-telugu">
                  ఐక్య
                </span>
              </div>
              <p className="text-[11px] text-slate-400 truncate">
                {locale === 'te' ? 'మన సంఘం, మన ఐక్యత' : 'Community Living, Unified'}
              </p>
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={toggleSidebar}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          title={sidebarCollapsed ? 'Expand' : 'Collapse'}
        >
          {sidebarCollapsed ? (
            <ChevronRight className="w-4 h-4" />
          ) : (
            <ChevronLeft className="w-4 h-4" />
          )}
        </button>
      </div>

      {/* Active Tenant Association Card */}
      {!sidebarCollapsed && activeAssociation && (
        <div className="mx-3 mt-4 p-3 rounded-xl bg-slate-800/70 border border-slate-700/70">
          <div className="flex items-start gap-2.5">
            <div className="p-2 rounded-lg bg-indigo-500/20 text-indigo-300 shrink-0 mt-0.5">
              <Building2 className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-white truncate">
                {activeAssociation.associationName}
              </p>
              <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5 truncate">
                <MapPin className="w-3 h-3 text-orange-400 shrink-0" />
                {activeAssociation.schemaName === 'aikya_assoc_1'
                  ? locale === 'te'
                    ? 'ఒంగోలు, ఆంధ్రప్రదేశ్ (523001)'
                    : 'Ongole, AP (523001)'
                  : locale === 'te'
                  ? 'గచ్చిబౌలి, హైదరాబాద్'
                  : 'Gachibowli, Hyderabad'}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-1.5">
                <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-mono bg-slate-900 text-emerald-400 border border-slate-700">
                  {activeAssociation.schemaName}
                </span>
                <Badge variant="saffron" size="xs">
                  {t(`roles.${roleKey}`, roleKey)}
                </Badge>
                {activeAssociation.unitNumber && (
                  <span className="text-[10px] text-slate-300 font-medium bg-slate-700/80 px-1.5 py-0.5 rounded">
                    {activeAssociation.unitNumber}
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;

          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setActiveTab(item.id)}
              title={sidebarCollapsed ? t(item.labelKey) : undefined}
              className={clsx(
                'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 group cursor-pointer',
                isActive
                  ? 'bg-gradient-to-r from-indigo-600 to-indigo-500 text-white shadow-md shadow-indigo-950/50'
                  : 'text-slate-300 hover:bg-slate-800/80 hover:text-white',
                sidebarCollapsed && 'justify-center px-2'
              )}
            >
              <Icon
                className={clsx(
                  'w-5 h-5 shrink-0 transition-colors',
                  isActive
                    ? 'text-orange-300'
                    : 'text-slate-400 group-hover:text-slate-200'
                )}
              />
              {!sidebarCollapsed && (
                <span className="truncate flex-1 text-left">
                  {t(item.labelKey)}
                </span>
              )}
              {!sidebarCollapsed &&
                item.treasurerHighlight &&
                (roleKey === 'TREASURER' || roleKey === 'PRESIDENT') && (
                  <span className="w-2 h-2 rounded-full bg-orange-400 shrink-0" />
                )}
            </button>
          );
        })}
      </nav>

      {/* Footer Multi-Tenant Isolation Indicator */}
      <div className="p-3 border-t border-slate-800/80">
        {sidebarCollapsed ? (
          <div
            className="flex justify-center py-1 text-emerald-400"
            title={`Schema: ${activeAssociation?.schemaName || 'aikya_assoc_1'}`}
          >
            <ShieldCheck className="w-5 h-5" />
          </div>
        ) : (
          <div className="px-2.5 py-2 rounded-xl bg-slate-950/70 border border-slate-800 text-[11px] text-slate-400 flex items-center justify-between">
            <div className="flex items-center gap-2 truncate">
              <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
              <span className="truncate">PostgreSQL Schema Isolation</span>
            </div>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shrink-0" />
          </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
