import { defineConfig, devices } from '@playwright/test'

// Video recording per CLAUDE.md rule 9 (project-version-completion e2e recording).
// Playwright writes one .webm per test under outputDir; scripts/collect-e2e-video.mjs
// copies them into root .recordings/ afterward (Playwright has no single-file-name option).
export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
    video: 'on',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Story 6.1's video-join e2e needs a real (fake) camera/mic so
        // getUserMedia resolves instead of prompting — same approach as any
        // WebRTC testing in headless Chromium.
        permissions: ['camera', 'microphone'],
        launchOptions: {
          args: ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'],
        },
      },
    },
  ],
})
