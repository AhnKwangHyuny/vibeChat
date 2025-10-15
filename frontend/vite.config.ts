import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
    server: {
      host: '0.0.0.0',  // 외부 기기(모바일) 접속 허용
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
          cookieDomainRewrite: 'localhost',
          cookiePathRewrite: '/',
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
