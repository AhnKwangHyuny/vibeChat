import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
        '/ws': {
          target: 'http://localhost:8080',
          ws: true,
          changeOrigin: true
        }
      }
    },

  define: {
    'global': 'window',
  },
  build: {
    rollupOptions: {
      onwarn(warning, warn) {

        if (warning.code === 'TYPESCRIPT_ERROR') return;
        warn(warning);
      }
    }
  }
});
