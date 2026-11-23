import { GoogleGenAI, Type } from '@google/genai';
import type { VercelRequest, VercelResponse } from '@vercel/node';

export const GEMINI_MODEL = 'gemini-2.5-flash';

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const MAX_BASE64_LENGTH = 4_800_000; // ~3.5MB decoded, well within Vercel 4.5MB payload limit

// Simple per-instance sliding window rate limit
const rateWindowMs = 60_000;
const maxRequestsPerWindow = 30;
const ipHits = new Map<string, number[]>();

export function checkRateLimit(req: VercelRequest, res: VercelResponse): boolean {
  const forwarded = req.headers['x-forwarded-for'];
  const ip = (Array.isArray(forwarded) ? forwarded[0] : forwarded)?.split(',')[0]?.trim() || 'unknown';
  const now = Date.now();
  const timestamps = (ipHits.get(ip) ?? []).filter((t) => now - t < rateWindowMs);
  if (timestamps.length >= maxRequestsPerWindow) {
    res.status(429).json({ error: 'Rate limit exceeded. Please wait a minute before retrying.' });
    return false;
  }
  timestamps.push(now);
  ipHits.set(ip, timestamps);
  return true;
}

export function requirePostAndClient(req: VercelRequest, res: VercelResponse): GoogleGenAI | null {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed. Use POST.' });
    return null;
  }
  if (!checkRateLimit(req, res)) {
    return null;
  }
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey.trim().length === 0) {
    res.status(503).json({
      error: 'GEMINI_API_KEY is not configured on the server. Add it in Vercel Project Settings -> Environment Variables.',
    });
    return null;
  }
  return new GoogleGenAI({ apiKey: apiKey.trim() });
}

export function parseValidatedImage(
  imageBase64: unknown,
  mimeType: unknown
): { data: string; mimeType: string } | { error: string } {
  if (typeof imageBase64 !== 'string' || imageBase64.trim().length === 0) {
    return { error: 'Missing image payload.' };
  }
  const cleanMime = typeof mimeType === 'string' ? mimeType.toLowerCase().trim() : 'image/jpeg';
  if (!ALLOWED_MIME_TYPES.has(cleanMime)) {
    return { error: 'Unsupported image format. Please upload JPEG, PNG, or WebP.' };
  }
  const stripped = imageBase64.includes(',') ? imageBase64.split(',')[1] ?? '' : imageBase64;
  if (stripped.length === 0 || stripped.length > MAX_BASE64_LENGTH) {
    return { error: 'Image size exceeds the 3.5MB upload limit. Please use a smaller photo.' };
  }
  return { data: stripped, mimeType: cleanMime };
}

export function parseJsonSafe(raw: string | undefined): Record<string, unknown> {
  if (!raw) throw new Error('Gemini returned an empty response.');
  const cleaned = raw.replace(/^```json\s*/i, '').replace(/```\s*$/i, '').trim();
  return JSON.parse(cleaned) as Record<string, unknown>;
}

export { Type };
