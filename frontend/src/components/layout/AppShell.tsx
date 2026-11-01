import React from 'react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { MobileBottomNav } from './MobileBottomNav';
import { LoginModal } from './LoginModal';
import { useAppStore } from '../../store/useAppStore';

export interface AppShellProps {
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({ children }) => {
  const { locale } = useAppStore();

  return (
    <div
      className="min-h-screen flex bg-slate-50 text-slate-900"
      lang={locale}
    >
      {/* Desktop Collapsible Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 pb-16 md:pb-0">
        <TopBar />

        <main className="flex-1 px-4 sm:px-6 lg:px-8 py-6 max-w-7xl w-full mx-auto">
          {children}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <MobileBottomNav />

      {/* Interactive Passwordless Phone + OTP Modal */}
      <LoginModal />
    </div>
  );
};

export default AppShell;
