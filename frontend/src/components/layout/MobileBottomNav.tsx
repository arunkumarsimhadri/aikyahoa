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
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAppStore, AppTab } from '../../store/useAppStore';

interface MobileNavItem {
  id: AppTab;
  labelKey: string;
  icon: React.ComponentType<{ className?: string }>;
}

const MOBILE_ITEMS: MobileNavItem[] = [
  { id: 'dashboard', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { id: 'billing', labelKey: 'nav.billing', icon: ReceiptIndianRupee },
  { id: 'reconciliation', labelKey: 'nav.reconciliation', icon: CheckCircle2 },
  { id: 'helpdesk', labelKey: 'nav.helpdesk', icon: Wrench },
  { id: 'amenities', labelKey: 'nav.amenities', icon: CalendarClock },
  { id: 'governance', labelKey: 'nav.governance', icon: Vote },
  { id: 'notices', labelKey: 'nav.notices', icon: Megaphone },
];

export const MobileBottomNav: React.FC = () => {
  const { t } = useTranslation();
  const { activeTab, setActiveTab } = useAppStore();

  return (
    <nav
      className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 px-1 py-1.5 shadow-lg"
      aria-label="Mobile Navigation"
    >
      <div className="flex items-center justify-around overflow-x-auto no-scrollbar">
        {MOBILE_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setActiveTab(item.id)}
              className={clsx(
                'flex flex-col items-center justify-center min-w-[52px] px-1.5 py-1 rounded-xl text-[10px] font-medium transition-colors',
                isActive
                  ? 'text-indigo-600 bg-indigo-50/80 font-semibold'
                  : 'text-slate-500 hover:text-slate-900'
              )}
            >
              <Icon
                className={clsx(
                  'w-4 h-4 mb-0.5',
                  isActive ? 'text-indigo-600' : 'text-slate-400'
                )}
              />
              <span className="truncate max-w-[60px]">{t(item.labelKey)}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

export default MobileBottomNav;
