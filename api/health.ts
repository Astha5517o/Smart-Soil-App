import type { VercelRequest, VercelResponse } from '@vercel/node';
import { GoogleGenAI } from '@google/genai';
import {
  GEMINI_FALLBACK_MODEL,
  GEMINI_MODEL,
  generateWithModelFallback,
} from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET' && req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed. Use GET.' });
  }

  const rawKey = process.env.GEMINI_API_KEY;
  const hasGeminiKey = typeof rawKey === 'string' && rawKey.trim().length > 0;
  const shouldProbe =
    req.url?.includes('probe=1') ||
    (req.query && String(req.query.probe || '') === '1');

  if (!hasGeminiKey) {
    return res.status(200).json({
      ok: false,
      apiReachable: true,
      hasGeminiKey: false,
      model: GEMINI_MODEL,
      fallbackModel: GEMINI_FALLBACK_MODEL,
      message:
        'Backend API is reachable, but GEMINI_API_KEY is not set. Add GEMINI_API_KEY in Vercel Environment Variables (or .env.local for local dev).',
    });
  }

  if (!shouldProbe) {
    return res.status(200).json({
      ok: true,
      apiReachable: true,
      hasGeminiKey: true,
      model: GEMINI_MODEL,
      fallbackModel: GEMINI_FALLBACK_MODEL,
      message: `API connected (${GEMINI_MODEL}).`,
    });
  }

  try {
    const ai = new GoogleGenAI({ apiKey: rawKey.trim() });
    await generateWithModelFallback(ai, {
      contents: 'Reply with OK.',
      config: { maxOutputTokens: 8 },
    });
    return res.status(200).json({
      ok: true,
      apiReachable: true,
      hasGeminiKey: true,
      liveVerified: true,
      model: GEMINI_MODEL,
      fallbackModel: GEMINI_FALLBACK_MODEL,
      message: `Gemini API connected and verified (${GEMINI_MODEL}).`,
    });
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err || 'Unknown error');
    return res.status(502).json({
      ok: false,
      apiReachable: true,
      hasGeminiKey: true,
      liveVerified: false,
      model: GEMINI_MODEL,
      fallbackModel: GEMINI_FALLBACK_MODEL,
      message: `API key is configured, but Gemini test call failed: ${detail.slice(0, 140)}`,
    });
  }
}
