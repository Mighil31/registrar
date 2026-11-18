/// <reference types="vitest/config" />
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Dev: the API is proxied, so the app and /api share one origin (as behind nginx in compose),
// and the SameSite=Strict refresh cookie works.
export default defineConfig({
  plugins: [tanstackRouter({ target: "react", autoCodeSplitting: true }), react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: { "/api": "http://localhost:8082" },
  },
  test: { environment: "node", include: ["src/**/*.test.ts"] },
});
