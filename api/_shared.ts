import { GoogleGenAI, Type } from '@google/genai';
import type { VercelRequest, VercelResponse } from '@vercel/node';

export const GEMINI_MODEL = 'gemini-3.8-flash';
export const GEMINI_FALLBACK_MODEL = 'gemini-3.5-flash-lite';

const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const MAX_BASE64_LENGTH = 4_000_000; // ~3.0MB decoded, safely under Vercel 4.5MB payload limit

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
    res.status(429).json({ error: 'Too many requests. Please wait a minute before trying again.' });
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

type GenerateContentParams = Parameters<GoogleGenAI['models']['generateContent']>[0];
type GenerateContentResult = Awaited<ReturnType<GoogleGenAI['models']['generateContent']>>;

export async function generateWithModelFallback(
  ai: GoogleGenAI,
  params: Omit<GenerateContentParams, 'model'>
): Promise<GenerateContentResult> {
  try {
    return await ai.models.generateContent({
      ...params,
      model: GEMINI_MODEL,
    });
  } catch (err) {
    const status = typeof err === 'object' && err !== null ? (err as { status?: unknown }).status : undefined;
    const msg = (err instanceof Error ? err.message : String(err || '')).toLowerCase();
    const isModelError =
      status === 404 ||
      msg.includes('not found') ||
      msg.includes('404') ||
      msg.includes('unsupported') ||
      msg.includes('deprecated') ||
      msg.includes('no longer available');
    if (!isModelError) {
      throw err;
    }
    console.warn(`Primary model ${GEMINI_MODEL} unavailable (${msg}); retrying with ${GEMINI_FALLBACK_MODEL}`);
    return await ai.models.generateContent({
      ...params,
      model: GEMINI_FALLBACK_MODEL,
    });
  }
}

export function parseRequestBody(req: VercelRequest): Record<string, unknown> {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try {
      const parsed = JSON.parse(req.body);
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : {};
    } catch {
      return {};
    }
  }
  if (typeof req.body === 'object' && !Array.isArray(req.body)) {
    return req.body as Record<string, unknown>;
  }
  return {};
}

export function parseValidatedImage(
  imageBase64: unknown,
  mimeType: unknown
): { data: string; mimeType: string } | { error: string } {
  if (typeof imageBase64 !== 'string' || imageBase64.trim().length === 0) {
    return { error: 'Missing image payload. Please choose or take a photo first.' };
  }

  let cleanMime = typeof mimeType === 'string' ? mimeType.toLowerCase().trim() : 'image/jpeg';
  if (cleanMime === 'image/jpg' || cleanMime === '') {
    cleanMime = 'image/jpeg';
  }
  if (!ALLOWED_MIME_TYPES.has(cleanMime)) {
    return { error: 'Unsupported image format. Please upload JPEG, PNG, or WebP.' };
  }

  const stripped = (imageBase64.includes(',') ? imageBase64.split(',')[1] ?? '' : imageBase64).trim();
  if (stripped.length === 0) {
    return { error: 'Image data is empty. Please choose another photo.' };
  }
  if (stripped.length > MAX_BASE64_LENGTH) {
    return { error: 'Image size exceeds the 3MB upload limit. Please use a smaller photo.' };
  }
  if (!/^[A-Za-z0-9+/=\s]+$/.test(stripped)) {
    return { error: 'Image encoding is invalid. Please upload the photo again.' };
  }
  return { data: stripped, mimeType: cleanMime };
}

export function extractResponseText(response: {
  text?: string;
  candidates?: Array<{
    finishReason?: string;
    content?: { parts?: Array<{ text?: string; thought?: boolean }> };
  }>;
}): string {
  if (typeof response.text === 'string' && response.text.trim().length > 0) {
    return response.text;
  }
  const partsText = response.candidates?.[0]?.content?.parts
    ?.filter((p) => !p.thought)
    .map((p) => (typeof p.text === 'string' ? p.text : ''))
    .join('')
    .trim();
  if (partsText) {
    return partsText;
  }
  const finishReason = response.candidates?.[0]?.finishReason;
  if (finishReason && finishReason !== 'STOP') {
    throw new Error(`Gemini stopped early (finishReason: ${finishReason}).`);
  }
  return '';
}

export function parseJsonSafe(raw: string | undefined): Record<string, unknown> {
  if (!raw || !raw.trim()) {
    throw new Error('Gemini returned an empty response.');
  }
  const cleaned = raw.replace(/^```(?:json)?\s*/i, '').replace(/```\s*$/i, '').trim();
  let parsed: unknown;
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    const start = cleaned.indexOf('{');
    const end = cleaned.lastIndexOf('}');
    if (start >= 0 && end > start) {
      parsed = JSON.parse(cleaned.slice(start, end + 1));
    } else {
      throw new Error('Gemini returned malformed JSON.');
    }
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('Gemini returned an unexpected response structure.');
  }
  return parsed as Record<string, unknown>;
}

export function handleGeminiError(
  res: VercelResponse,
  contextLabel: string,
  err: unknown,
  fallbackMessage: string
) {
  const rawMessage = err instanceof Error ? err.message : String(err || 'Unknown error');
  console.error(`${contextLabel} failed:`, rawMessage);

  const lower = rawMessage.toLowerCase();
  if (
    lower.includes('api key') ||
    lower.includes('api_key_invalid') ||
    lower.includes('401') ||
    lower.includes('403') ||
    lower.includes('permission_denied')
  ) {
    return res.status(502).json({
      error: 'Gemini API key is invalid or unauthorized. Verify GEMINI_API_KEY in Vercel Environment Variables.',
    });
  }
  if (lower.includes('429') || lower.includes('quota') || lower.includes('resource_exhausted')) {
    return res.status(429).json({
      error: 'Gemini API rate limit or quota reached. Please wait a minute and try again.',
    });
  }
  if (lower.includes('safety') || lower.includes('blocked')) {
    return res.status(422).json({
      error: 'This photo could not be processed safely. Please try a clearer daylight photo of soil, leaves, or your report.',
    });
  }
  if (lower.includes('404') || (lower.includes('model') && lower.includes('not found'))) {
    return res.status(502).json({
      error: 'Configured Gemini model was unavailable for your API key. Please check model access in Google AI Studio.',
    });
  }
  if (lower.includes('503') || lower.includes('unavailable') || lower.includes('overloaded')) {
    return res.status(503).json({
      error: 'Gemini AI service is temporarily busy. Please try again in a few seconds.',
    });
  }
  if (lower.includes('invalid_argument') || lower.includes('400')) {
    return res.status(400).json({
      error: 'The photo format or request was rejected by Gemini. Please try a standard JPEG or PNG photo.',
    });
  }
  const cleanDetail = rawMessage.replace(/\s+/g, ' ').trim().slice(0, 140);
  return res.status(502).json({
    error: cleanDetail ? `${fallbackMessage} (${cleanDetail})` : fallbackMessage,
  });
}

export function normalizeString(value: unknown, fallback: string): string {
  if (typeof value === 'string' && value.trim().length > 0) {
    return value.trim();
  }
  return fallback;
}

export function normalizeStringArray(value: unknown, fallback: string[] = []): string[] {
  if (!Array.isArray(value)) return fallback;
  const items = value
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim());
  return items.length > 0 ? items : fallback;
}

export function isUnreadableToken(value: string): boolean {
  const lower = value.trim().toLowerCase();
  return (
    lower.length === 0 ||
    lower === 'not readable' ||
    lower === 'unreadable' ||
    lower === 'unclear' ||
    lower === 'missing' ||
    lower === 'n/a' ||
    lower === 'unknown'
  );
}

export { Type };
