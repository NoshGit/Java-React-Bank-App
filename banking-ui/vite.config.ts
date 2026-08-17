import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Forward all backend paths to the BFF on port 8080.
      // The browser only ever talks to localhost:5173 in development.
      '/api':     { target: 'http://localhost:8080', changeOrigin: false },
      '/oauth2':  { target: 'http://localhost:8080', changeOrigin: false },
      '/login':   { target: 'http://localhost:8080', changeOrigin: false },
      '/logout':  { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
});