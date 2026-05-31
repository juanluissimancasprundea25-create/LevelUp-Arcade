import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// Vite genera los estáticos en src/main/resources/static/app/
// para que Spring Boot los sirva en /app/* y el SpaController
// haga forward de cualquier otra ruta a index.html.
export default defineConfig({
  plugins: [react()],
  base: '/app/',
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static/app'),
    emptyOutDir: true,
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
  },
  server: {
    port: 5173,
    proxy: {
      // En dev, Vite escucha en :5173 y Spring Boot en :8080.
      // Reenviamos /api al backend para que JWT y CORS no estorben.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
