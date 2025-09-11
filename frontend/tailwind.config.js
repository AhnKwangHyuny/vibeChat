/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: 'rgb(247, 248, 248)',
          100: 'rgb(208, 214, 224)',
          200: 'rgb(138, 143, 152)',
          300: 'rgb(98, 102, 109)',
          400: 'rgb(62, 62, 68)',
          500: 'rgb(20, 21, 22)',
          900: 'rgb(8, 9, 10)'
        },
        background: {
          primary: 'rgb(8, 9, 10)',
          secondary: 'rgb(20, 21, 22)',
          tertiary: 'rgba(255, 255, 255, 0.05)',
          chat: 'rgb(15, 16, 17)',
          'chat-message': 'rgb(25, 26, 27)',
          'chat-input': 'rgb(18, 19, 20)',
          transparent: 'rgba(0, 0, 0, 0)'
        },
        foreground: {
          primary: 'rgb(247, 248, 248)',
          secondary: 'rgb(208, 214, 224)',
          muted: 'rgb(138, 143, 152)',
          disabled: 'rgb(98, 102, 109)'
        },
        border: {
          default: 'rgba(255, 255, 255, 0.05)',
          focus: 'rgb(62, 62, 68)',
          dashed: 'rgb(62, 62, 68)'
        },
        semantic: {
          success: 'rgb(34, 197, 94)',
          warning: 'rgb(251, 191, 36)',
          error: 'rgb(239, 68, 68)',
          info: 'rgb(59, 130, 246)'
        }
      },
      fontFamily: {
        sans: ['"Inter Variable"', '"SF Pro Display"', '-apple-system', '"system-ui"', '"Segoe UI"', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', '"Open Sans"', '"Helvetica Neue"', 'sans-serif']
      },
      fontSize: {
        xs: '13px',
        sm: '14px',
        base: '16px',
        lg: '17px',
        xl: '21px',
        '2xl': '24px',
        '3xl': '56px',
        '4xl': '64px'
      },
      fontWeight: {
        normal: '400',
        medium: '510',
        semibold: '538'
      },
      lineHeight: {
        tight: '24px',
        normal: '27.93px',
        relaxed: '31.92px',
        loose: '61.6px',
        'extra-loose': '67.84px'
      },
      spacing: {
        '0': '0px',
        '1': '4px',
        '2': '8px',
        '3': '12px',
        '4': '16px',
        '6': '24px',
        '8': '32px',
        '12': '48px',
        '16': '64px',
        '18': '72px'
      },
      borderRadius: {
        none: '0px',
        sm: '4px',
        base: '8px',
        lg: '30px',
        full: '9999px'
      },
      boxShadow: {
        none: 'none',
        sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        base: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
        lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)'
      },
      animation: {
        'fade-in': 'fadeIn 0.1s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
        'slide-up': 'slideUp 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
        'scale-in': 'scaleIn 0.1s cubic-bezier(0.25, 0.46, 0.45, 0.94)'
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        scaleIn: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        }
      }
    },
  },
  plugins: [],
}