import { existsSync } from "node:fs";
import { defineConfig, devices } from "@playwright/test";

// End-to-end journeys against a running stack with demo data:
//   docker compose up -d   (with DEV_SEED=true in .env)
//   npm run e2e
// E2E_BASE_URL points elsewhere (e.g. http://localhost:5173 for the Vite dev server).

const systemChromium = process.env.CHROMIUM_PATH ?? "/usr/bin/chromium";

export default defineConfig({
  testDir: "e2e",
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:8083",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        // Use the system's Chromium when there is one; otherwise `npx playwright install chromium`.
        launchOptions: existsSync(systemChromium) ? { executablePath: systemChromium } : {},
      },
    },
  ],
});
