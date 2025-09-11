import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    hmr: {
      clientPort: 5173
    }
  },
  define: {
    'global': 'window',
  },
  build: {
    rollupOptions: {
      onwarn(warning, warn) {
        // TypeScript 에러 무시
        if (warning.code === 'TYPESCRIPT_ERROR') return;
        warn(warning);
      }
    }
  }
})
