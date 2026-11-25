# FarmerBuddy (SMART SOIL) — Vercel Deployment & Local Development Guide

## Why Deployment Failed Previously
The repository initially contained only the Android Gradle project (`app/`, `build.gradle.kts`) with no `package.json`, `dist` web build, `vercel.json`, or `/api` serverless functions. Vercel requires a web project configuration (`package.json`, build output, and Node serverless routes) at the repository root.

## Added Vercel Web Structure
- `package.json` — Vite + React + TypeScript + `@google/genai` + `@vercel/node`
- `vercel.json` — Configures Vite `dist` output and SPA rewrites while keeping `/api/*` serverless routes intact
- `api/_shared.ts` — Server-side `GEMINI_API_KEY` check, rate limiting, and image validation
- `api/analyze-soil.ts`, `api/identify-crop.ts`, `api/plant-health.ts`, `api/npk-extract.ts`, `api/npk-regional.ts`, `api/advisor.ts` — Secure serverless endpoints
- `src/` — FarmerBuddy (SMART SOIL) React + TypeScript frontend with Web Bluetooth (`navigator.bluetooth`) and local-first `localStorage` reports

## Required Environment Variable in Vercel
1. Open your project in **Vercel Dashboard → Settings → Environment Variables**.
2. Add:
   - **Name:** `GEMINI_API_KEY`
   - **Value:** *(your Gemini API key from Google AI Studio)*
   - **Environments:** Production, Preview, and Development
3. Never prefix this variable with `VITE_` and never paste it into `src/` or GitHub.

## GitHub → Vercel Deployment Steps
1. Commit and push the updated repository (`package.json`, `vercel.json`, `api/`, `src/`, `index.html`, `tsconfig.json`, `vite.config.ts`) to GitHub.
2. In Vercel, import the GitHub repository with:
   - **Framework Preset:** Vite
   - **Root Directory:** `./`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
3. Add `GEMINI_API_KEY` in Environment Variables and click **Deploy**.

## Local Web Development
```bash
npm install
# Create .env.local with GEMINI_API_KEY=your_key_here
npx vercel dev
```
