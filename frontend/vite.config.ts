/// <reference types="vitest/config" />
import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    exclude: ['e2e/**', 'node_modules/**'],
    // Tests never hit a real backend — MSW intercepts every request regardless of the URL — but
    // apiClient.baseUrl still needs to resolve to *something* absolute, or the underlying fetch
    // throws on a bare "/api/v1/..." path. Previously this only worked locally by accident, via
    // each developer's own untracked .env.local providing VITE_API_BASE_URL; there was no fallback
    // for a clean checkout (e.g. CI) without one. Set explicitly here so the test suite never
    // depends on local environment files at all.
    env: {
      VITE_API_BASE_URL: 'http://localhost:3000',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/features/**', 'src/shared/**'],
      exclude: ['src/shared/components/ui/**', 'src/shared/api/schema.d.ts'],
      // CLAUDE.md rule 6's 80% gate, enforced automatically (mirrors backend's
      // jacocoTestCoverageVerification) rather than manually eyeballing the printed summary.
      // Statements/lines only, matching how this gate has been tracked and reported throughout
      // this project — branch coverage has consistently sat lower (~70%) without ever being the
      // enforced metric, same asymmetry jacoco's own single "minimum" rule has on the backend side.
      thresholds: {
        statements: 80,
        lines: 80,
      },
    },
  },
})
