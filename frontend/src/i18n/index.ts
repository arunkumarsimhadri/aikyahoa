import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from '../locales/en.json';
import te from '../locales/te.json';
import type { LocaleCode } from '../types/api';

const savedLocale = (typeof window !== 'undefined' && localStorage.getItem('aikya_locale')) as LocaleCode | null;
const initialLocale: LocaleCode = savedLocale === 'te' || savedLocale === 'en' ? savedLocale : 'en';

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    te: { translation: te },
  },
  lng: initialLocale,
  fallbackLng: 'en',
  interpolation: {
    escapeValue: false,
  },
});

if (typeof document !== 'undefined') {
  document.documentElement.lang = initialLocale;
  document.documentElement.setAttribute('data-locale', initialLocale);
}

export function changeAppLanguage(lang: LocaleCode): void {
  i18n.changeLanguage(lang);
  if (typeof window !== 'undefined') {
    localStorage.setItem('aikya_locale', lang);
  }
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lang;
    document.documentElement.setAttribute('data-locale', lang);
  }
}

export default i18n;
