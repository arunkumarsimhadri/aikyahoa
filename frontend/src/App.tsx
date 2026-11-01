import React, { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { AppShell } from './components/layout/AppShell';
import { useAppStore } from './store/useAppStore';
import { DashboardPage } from './pages/DashboardPage';
import { BillingAndReconciliationPage } from './pages/BillingAndReconciliationPage';
import { HelpdeskPage } from './pages/HelpdeskPage';
import { AmenitiesPage } from './pages/AmenitiesPage';
import { GovernancePage } from './pages/GovernancePage';
import { NoticesAndNotificationsPage } from './pages/NoticesAndNotificationsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const ActiveFeatureView: React.FC = () => {
  const { activeTab, locale } = useAppStore();
  const { i18n } = useTranslation();

  useEffect(() => {
    if (i18n.language !== locale) {
      i18n.changeLanguage(locale);
    }
  }, [locale, i18n]);

  switch (activeTab) {
    case 'dashboard':
      return <DashboardPage />;
    case 'billing':
    case 'reconciliation':
      return <BillingAndReconciliationPage />;
    case 'helpdesk':
      return <HelpdeskPage />;
    case 'amenities':
      return <AmenitiesPage />;
    case 'governance':
      return <GovernancePage />;
    case 'notices':
      return <NoticesAndNotificationsPage />;
    default:
      return <DashboardPage />;
  }
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AppShell>
        <ActiveFeatureView />
      </AppShell>
    </QueryClientProvider>
  );
};

export default App;
