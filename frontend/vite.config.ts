import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/tenants': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    watch: {
      // WSL2: inotify events don't propagate from Windows FS — use polling
      usePolling: true,
      interval: 300,
    },
    hmr: true,
  },
});
