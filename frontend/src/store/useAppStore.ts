import { create } from 'zustand';
import { changeAppLanguage } from '../i18n';
import type {
  AssociationMembershipDto,
  AuthTokenResponse,
  LocaleCode,
  UserDto,
} from '../types/api';

export type ActiveTab =
  | 'dashboard'
  | 'billing'
  | 'reconciliation'
  | 'helpdesk'
  | 'amenities'
  | 'governance'
  | 'notices';

export type DemoPersonaKey = 'treasurer' | 'owner' | 'tenant' | 'assoc2';

export interface DemoPersona {
  key: DemoPersonaKey;
  label: string;
  labelTe: string;
  badgeText: string;
  token: string;
  user: UserDto;
  activeAssociation: AssociationMembershipDto;
  memberships: AssociationMembershipDto[];
}

const ELITE_TOWERS_TREASURER_MEMBERSHIP: AssociationMembershipDto = {
  membershipId: 1,
  associationId: 1,
  associationName: 'Elite Towers',
  associationNameTe: 'ఏలైట్ టవర్స్ — ఒంగోలు',
  associationCode: 'ELITE_TOWERS_ONGOLE',
  schemaName: 'aikya_assoc_1',
  role: 'TREASURER',
  unitNumber: 'A-101',
  blockName: 'Block A',
};

const ELITE_TOWERS_OWNER_MEMBERSHIP: AssociationMembershipDto = {
  membershipId: 2,
  associationId: 1,
  associationName: 'Elite Towers',
  associationNameTe: 'ఏలైట్ టవర్స్ — ఒంగోలు',
  associationCode: 'ELITE_TOWERS_ONGOLE',
  schemaName: 'aikya_assoc_1',
  role: 'OWNER',
  unitNumber: 'A-102',
  blockName: 'Block A',
};

const ELITE_TOWERS_TENANT_MEMBERSHIP: AssociationMembershipDto = {
  membershipId: 3,
  associationId: 1,
  associationName: 'Elite Towers',
  associationNameTe: 'ఏలైట్ టవర్స్ — ఒంగోలు',
  associationCode: 'ELITE_TOWERS_ONGOLE',
  schemaName: 'aikya_assoc_1',
  role: 'TENANT',
  unitNumber: 'B-201',
  blockName: 'Block B',
};

const CYBER_MEADOWS_PRESIDENT_MEMBERSHIP: AssociationMembershipDto = {
  membershipId: 4,
  associationId: 2,
  associationName: 'Cyber Meadows RWA',
  associationNameTe: 'సైబర్ మెడోస్ అసోసియేషన్',
  associationCode: 'CYBER_MEADOWS_HYD',
  schemaName: 'aikya_assoc_2',
  role: 'PRESIDENT',
  unitNumber: 'C-301',
  blockName: 'Block C',
};

export const DEMO_PERSONAS: Record<DemoPersonaKey, DemoPersona> = {
  treasurer: {
    key: 'treasurer',
    label: 'Treasurer - Flat A-101 (Elite Towers)',
    labelTe: 'కోశాధికారి - ఫ్లాట్ A-101 (ఏలైట్ టవర్స్)',
    badgeText: 'TREASURER • A-101',
    token:
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcklkIjoxLCJwaG9uZU51bWJlciI6Iis5MTk4NDgwMTExMTEiLCJmdWxsTmFtZSI6IlJhbWEgUmFvIE5hbmRhbXVyaSIsInByZWZlcnJlZExvY2FsZSI6ImVuIiwiYXNzb2NpYXRpb25JZCI6MSwidGVuYW50U2NoZW1hIjoiYWlreWFfYXNzb2NfMSIsInJvbGUiOiJUUkVBU1VSRVIiLCJ1bml0TnVtYmVyIjoiQS0xMDEiLCJibG9ja05hbWUiOiJCbG9jayBBIiwiaWF0IjoxNzMwMDAwMDAwLCJleHAiOjE5MjQ5MDU2MDB9.h0jnxTe7sjlD7E3yr84zbssO-QOnTqx3D3hXE-N7XFo',
    user: {
      id: 1,
      phoneNumber: '+919848011111',
      fullName: 'Rama Rao Nandamuri',
      fullNameTe: 'నందమూరి రామారావు',
      email: 'treasurer@elitetowersongole.in',
      preferredLocale: 'en',
      active: true,
    },
    activeAssociation: ELITE_TOWERS_TREASURER_MEMBERSHIP,
    memberships: [
      ELITE_TOWERS_TREASURER_MEMBERSHIP,
      CYBER_MEADOWS_PRESIDENT_MEMBERSHIP,
    ],
  },
  owner: {
    key: 'owner',
    label: 'Owner - Flat A-102 (Elite Towers)',
    labelTe: 'యజమాని - ఫ్లాట్ A-102 (ఏలైట్ టవర్స్)',
    badgeText: 'OWNER • A-102',
    token:
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwidXNlcklkIjoyLCJwaG9uZU51bWJlciI6Iis5MTk4NDgwMjIyMjIiLCJmdWxsTmFtZSI6Ikxha3NobWkgRGV2aSBLb25pZGVsYSIsInByZWZlcnJlZExvY2FsZSI6ImVuIiwiYXNzb2NpYXRpb25JZCI6MSwidGVuYW50U2NoZW1hIjoiYWlreWFfYXNzb2NfMSIsInJvbGUiOiJPV05FUiIsInVuaXROdW1iZXIiOiJBLTEwMiIsImJsb2NrTmFtZSI6IkJsb2NrIEEiLCJpYXQiOjE3MzAwMDAwMDAsImV4cCI6MTkyNDkwNTYwMH0.-6z_x9Gy-MFjQYsrqlgFEnhgX9VgLdNuYBXDXwJNrRI',
    user: {
      id: 2,
      phoneNumber: '+919848022222',
      fullName: 'Lakshmi Devi Konidela',
      fullNameTe: 'కొనిదెల లక్ష్మీ దేవి',
      email: 'lakshmi.a102@elitetowersongole.in',
      preferredLocale: 'en',
      active: true,
    },
    activeAssociation: ELITE_TOWERS_OWNER_MEMBERSHIP,
    memberships: [
      ELITE_TOWERS_OWNER_MEMBERSHIP,
      CYBER_MEADOWS_PRESIDENT_MEMBERSHIP,
    ],
  },
  tenant: {
    key: 'tenant',
    label: 'Tenant - Flat B-201 (Elite Towers)',
    labelTe: 'అద్దెదారు - ఫ్లాట్ B-201 (ఏలైట్ టవర్స్)',
    badgeText: 'TENANT • B-201',
    token:
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIzIiwidXNlcklkIjozLCJwaG9uZU51bWJlciI6Iis5MTk4NDgwMzMzMzMiLCJmdWxsTmFtZSI6IkthcnRoaWtleWEgVmFybWEiLCJwcmVmZXJyZWRMb2NhbGUiOiJlbiIsImFzc29jaWF0aW9uSWQiOjEsInRlbmFudFNjaGVtYSI6ImFpa3lhX2Fzc29jXzEiLCJyb2xlIjoiVEVOQU5UIiwidW5pdE51bWJlciI6IkItMjAxIiwiYmxvY2tOYW1lIjoiQmxvY2sgQiIsImlhdCI6MTczMDAwMDAwMCwiZXhwIjoxOTI0OTA1NjAwfQ.1YlIlNJ8juo_eQlRC7QJNwhxmj5zchBux6OulXBtelo',
    user: {
      id: 3,
      phoneNumber: '+919848033333',
      fullName: 'Karthikeya Varma',
      fullNameTe: 'కార్తికేయ వర్మ',
      email: 'karthik.b201@elitetowersongole.in',
      preferredLocale: 'en',
      active: true,
    },
    activeAssociation: ELITE_TOWERS_TENANT_MEMBERSHIP,
    memberships: [
      ELITE_TOWERS_TENANT_MEMBERSHIP,
      CYBER_MEADOWS_PRESIDENT_MEMBERSHIP,
    ],
  },
  assoc2: {
    key: 'assoc2',
    label: 'President - Flat C-301 (Cyber Meadows RWA)',
    labelTe: 'అధ్యక్షులు - ఫ్లాట్ C-301 (సైబర్ మెడోస్)',
    badgeText: 'PRESIDENT • C-301',
    token:
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI0IiwidXNlcklkIjo0LCJwaG9uZU51bWJlciI6Iis5MTk4NDgwNDQ0NDQiLCJmdWxsTmFtZSI6IlNyaW5pdmFzIFJlZGR5IiwicHJlZmVycmVkTG9jYWxlIjoiZW4iLCJhc3NvY2lhdGlvbklkIjoyLCJ0ZW5hbnRTY2hlbWEiOiJhaWt5YV9hc3NvY18yIiwicm9sZSI6IlBSRVNJREVOVCIsInVuaXROdW1iZXIiOiJDLTMwMSIsImJsb2NrTmFtZSI6IkJsb2NrIEMiLCJpYXQiOjE3MzAwMDAwMDAsImV4cCI6MTkyNDkwNTYwMH0.H6yASV0LpBV5wHjfwXT4I3hCFc12DfFxlgvUBrYvRu0',
    user: {
      id: 4,
      phoneNumber: '+919848044444',
      fullName: 'Srinivas Reddy',
      fullNameTe: 'శ్రీనివాస్ రెడ్డి',
      email: 'president@cybermeadows.in',
      preferredLocale: 'en',
      active: true,
    },
    activeAssociation: CYBER_MEADOWS_PRESIDENT_MEMBERSHIP,
    memberships: [
      ELITE_TOWERS_TREASURER_MEMBERSHIP,
      CYBER_MEADOWS_PRESIDENT_MEMBERSHIP,
    ],
  },
};

export const DEMO_PERSONA_LIST: DemoPersona[] = [
  DEMO_PERSONAS.treasurer,
  DEMO_PERSONAS.owner,
  DEMO_PERSONAS.tenant,
  DEMO_PERSONAS.assoc2,
];

export interface AppState {
  activeTab: ActiveTab;
  locale: LocaleCode;
  token: string | null;
  user: UserDto | null;
  activeAssociation: AssociationMembershipDto | null;
  memberships: AssociationMembershipDto[];
  sidebarCollapsed: boolean;
  loginModalOpen: boolean;
  activePersonaKey: DemoPersonaKey;

  setActiveTab: (tab: ActiveTab) => void;
  setLocale: (locale: LocaleCode) => void;
  setAuth: (
    authData:
      | AuthTokenResponse
      | {
          accessToken?: string;
          token?: string;
          user: UserDto;
          activeAssociation?: AssociationMembershipDto;
          memberships?: AssociationMembershipDto[];
        }
  ) => void;
  setActiveAssociation: (association: AssociationMembershipDto) => void;
  switchPersona: (personaKeyOrRole: DemoPersonaKey | string) => void;
  toggleSidebar: () => void;
  setLoginModalOpen: (open: boolean) => void;
  logout: () => void;
}

const initialLocale: LocaleCode =
  typeof window !== 'undefined' && localStorage.getItem('aikya_locale') === 'te'
    ? 'te'
    : 'en';

const defaultPersona = DEMO_PERSONAS.treasurer;

export const useAppStore = create<AppState>((set, get) => ({
  activeTab: 'dashboard',
  locale: initialLocale,
  token: defaultPersona.token,
  user: { ...defaultPersona.user, preferredLocale: initialLocale },
  activeAssociation: defaultPersona.activeAssociation,
  memberships: defaultPersona.memberships,
  sidebarCollapsed: false,
  loginModalOpen: false,
  activePersonaKey: 'treasurer',

  setActiveTab: (tab) => set({ activeTab: tab }),

  setLocale: (locale) => {
    changeAppLanguage(locale);
    const currentUser = get().user;
    set({
      locale,
      user: currentUser ? { ...currentUser, preferredLocale: locale } : null,
    });
  },

  setAuth: (authData) => {
    const rawAny = authData as unknown as Record<string, unknown>;
    const token =
      (rawAny.accessToken as string) ||
      (rawAny.token as string) ||
      get().token ||
      defaultPersona.token;
    const memberships =
      (authData.memberships && authData.memberships.length > 0
        ? authData.memberships
        : get().memberships) || defaultPersona.memberships;

    const activeAssoc =
      authData.activeAssociation ||
      memberships.find((m) => m.associationId === (rawAny.activeAssociationId as number)) ||
      memberships.find((m) => m.schemaName === (rawAny.activeTenantSchema as string)) ||
      memberships[0] ||
      defaultPersona.activeAssociation;

    const preferredLocale: LocaleCode =
      authData.user?.preferredLocale === 'te' ? 'te' : get().locale;

    changeAppLanguage(preferredLocale);

    set({
      token,
      user: authData.user,
      activeAssociation: activeAssoc,
      memberships,
      locale: preferredLocale,
      loginModalOpen: false,
    });
  },

  setActiveAssociation: (association) => {
    // If switching to aikya_assoc_2, use assoc2 persona token so JWT tenantSchema matches
    if (association.schemaName === 'aikya_assoc_2') {
      const p2 = DEMO_PERSONAS.assoc2;
      set({
        activeAssociation: association,
        token: p2.token,
        user: { ...p2.user, preferredLocale: get().locale },
        activePersonaKey: 'assoc2',
      });
      return;
    }
    const currentKey = get().activePersonaKey === 'assoc2' ? 'treasurer' : get().activePersonaKey;
    const persona = DEMO_PERSONAS[currentKey] || DEMO_PERSONAS.treasurer;
    set({
      activeAssociation: persona.activeAssociation,
      token: persona.token,
      user: { ...persona.user, preferredLocale: get().locale },
      activePersonaKey: persona.key,
    });
  },

  switchPersona: (personaKeyOrRole) => {
    const normalized = personaKeyOrRole.toLowerCase();
    let targetKey: DemoPersonaKey = 'treasurer';

    if (normalized === 'owner' || normalized.includes('a-102')) {
      targetKey = 'owner';
    } else if (normalized === 'tenant' || normalized.includes('b-201')) {
      targetKey = 'tenant';
    } else if (
      normalized === 'assoc2' ||
      normalized === 'president' ||
      normalized.includes('aikya_assoc_2') ||
      normalized.includes('cyber')
    ) {
      targetKey = 'assoc2';
    } else {
      targetKey = 'treasurer';
    }

    const persona = DEMO_PERSONAS[targetKey];
    const currentLocale = get().locale;

    set({
      activePersonaKey: targetKey,
      token: persona.token,
      user: { ...persona.user, preferredLocale: currentLocale },
      activeAssociation: persona.activeAssociation,
      memberships: persona.memberships,
    });
  },

  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),

  setLoginModalOpen: (open) => set({ loginModalOpen: open }),

  logout: () => {
    // Reset to Resident Owner view or open login modal for seamless re-auth
    const fallback = DEMO_PERSONAS.owner;
    set({
      activePersonaKey: 'owner',
      token: fallback.token,
      user: { ...fallback.user, preferredLocale: get().locale },
      activeAssociation: fallback.activeAssociation,
      loginModalOpen: true,
    });
  },
}));

export default useAppStore;
