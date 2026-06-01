/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Paleta inspirada en el póster de LevelUp Arcade
        cockpit: {
          bg: '#020408',
          panel: 'rgba(10, 14, 28, 0.55)',
          line: 'rgba(120, 80, 220, 0.35)',
          neon: '#a855f7',      // purpura principal
          cyan: '#22d3ee',      // cyan acento
          amber: '#fbbf24',     // alerta stock bajo
          danger: '#ef4444',
          ok: '#34d399',
        },
      },
      fontFamily: {
        // Variable system, sin descargar nada de Google.
        display: ['Orbitron', 'Rajdhani', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      animation: {
        'scan': 'scan 6s linear infinite',
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'flicker': 'flicker 3.2s linear infinite',
      },
      keyframes: {
        scan: {
          '0%':   { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100%)' },
        },
        flicker: {
          '0%, 19.999%, 22%, 62.999%, 64%, 64.999%, 70%, 100%': { opacity: '1' },
          '20%, 21.999%, 63%, 63.999%, 65%, 69.999%': { opacity: '0.55' },
        },
      },
    },
  },
  plugins: [],
};
