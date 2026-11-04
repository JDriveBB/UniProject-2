import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Desarrollo: `npm run dev` en :5173 reenvía /api al núcleo en :8080 (mismo origen para cookies y CSRF).
// Producción: el build se copia dentro de la imagen del núcleo (decisión 25-1).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: { outDir: 'dist', sourcemap: false },
  test: { environment: 'node', include: ['src/**/*.test.ts'] },
});
