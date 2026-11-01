/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', '"Noto Sans Telugu"', '"Mandali"', 'system-ui', '-apple-system', 'sans-serif'],
        telugu: ['"Noto Sans Telugu"', '"Mandali"', '"Plus Jakarta Sans"', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', '"Noto Sans Telugu"', 'sans-serif'],
      },
      colors: {
        saffron: {
          50: '#fff8ed',
          100: '#ffefd4',
          200: '#ffdba8',
          300: '#ffc070',
          400: '#ff9a36',
          500: '#fe7c0f',
          600: '#ef6005',
          700: '#c64707',
          800: '#9d390e',
          900: '#7e310f',
        },
        aikya: {
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
          950: '#1e1b4b',
        },
        kumkum: {
          500: '#e11d48',
          600: '#be123c',
        },
        turmeric: {
          400: '#facc15',
          500: '#eab308',
        },
      },
      boxShadow: {
        'soft': '0 4px 20px -2px rgba(30, 27, 75, 0.06), 0 2px 6px -1px rgba(30, 27, 75, 0.04)',
        'glow-saffron': '0 0 20px -4px rgba(254, 124, 15, 0.35)',
        'glow-indigo': '0 0 20px -4px rgba(79, 70, 229, 0.35)',
      },
    },
  },
  plugins: [],
};
