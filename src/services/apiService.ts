import type {
  AiAdvisoryPlan,
  CropIdResult,
  NpkLabResult,
  NpkRegionalResult,
  PlantHealthResult,
  SoilScanResult,
} from '../types/soil';

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);
const ALLOWED_EXTENSIONS = /\.(jpe?g|png|webp|heic|heif)$/i;
const MAX_RAW_BYTES = 15 * 1024 * 1024;
const MAX_BASE64_LENGTH = 2_800_000;
const REQUEST_TIMEOUT_MS = 45_000;

export async function compressImageFile(
  file: File,
  maxDim = 1024
): Promise<{ base64: string; mimeType: string; previewUrl: string }> {
  if (!file || file.size === 0) {
    throw new Error('Selected file is empty. Please choose another photo.');
  }
  if (file.size > MAX_RAW_BYTES) {
    throw new Error('Selected file is larger than 15MB. Please pick a smaller photo.');
  }

  const normalizedType = (file.type || '').toLowerCase().trim();
  const hasExplicitDisallowedMime =
    normalizedType.length > 0 && !ALLOWED_TYPES.has(normalizedType) && !normalizedType.startsWith('image/');
  const hasBlankUnknownType = normalizedType.length === 0 && file.name.length > 0 && !ALLOWED_EXTENSIONS.test(file.name);

  if (hasExplicitDisallowedMime || hasBlankUnknownType) {
    throw new Error('Please choose a JPEG, PNG, or WebP photo (PDFs and documents are not supported).');
  }

  const loadImageElement = (src: string): Promise<HTMLImageElement> =>
    new Promise((resolve, reject) => {
      const el = new Image();
      el.onload = () => {
        if (el.naturalWidth <= 0 || el.naturalHeight <= 0) {
          reject(new Error('Image appears corrupted or empty. Please choose another photo.'));
        } else {
          resolve(el);
        }
      };
      el.onerror = () =>
        reject(new Error('This image format could not be decoded. Please upload a JPEG, PNG, or WebP photo.'));
      el.src = src;
    });

  let img: HTMLImageElement;
  const objectUrl = typeof URL !== 'undefined' && URL.createObjectURL ? URL.createObjectURL(file) : '';
  try {
    if (objectUrl) {
      img = await loadImageElement(objectUrl);
    } else {
      throw new Error('ObjectURL unavailable');
    }
  } catch {
    const dataUrl = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(new Error('Could not read the selected image file.'));
      reader.readAsDataURL(file);
    });
    if (!dataUrl.startsWith('data:')) {
      throw new Error('Could not read image data. Please pick another photo.');
    }
    img = await loadImageElement(dataUrl);
  } finally {
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl);
    }
  }

  const encodeAt = (dimension: number, quality: number): string => {
    const scale = Math.min(dimension / img.naturalWidth, dimension / img.naturalHeight, 1);
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Image compression is unavailable in this browser.');
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', quality);
  };

  let compressedUrl = encodeAt(maxDim, 0.82);
  if (compressedUrl.length > MAX_BASE64_LENGTH) {
    compressedUrl = encodeAt(800, 0.72);
  }
  if (compressedUrl.length > MAX_BASE64_LENGTH) {
    compressedUrl = encodeAt(640, 0.65);
  }
  if (compressedUrl.length > MAX_BASE64_LENGTH) {
    throw new Error('Compressed photo is still too large to upload. Please crop or take a smaller photo.');
  }

  return {
    base64: compressedUrl,
    mimeType: 'image/jpeg',
    previewUrl: compressedUrl,
  };
}

export async function createStorageThumbnail(dataUrl: string, maxDim = 320): Promise<string | undefined> {
  if (!dataUrl || !dataUrl.startsWith('data:image/')) return undefined;
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => (el.naturalWidth > 0 ? resolve(el) : reject(new Error('Invalid image')));
      el.onerror = () => reject(new Error('Invalid image'));
      el.src = dataUrl;
    });
    const scale = Math.min(maxDim / img.naturalWidth, maxDim / img.naturalHeight, 1);
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
    const ctx = canvas.getContext('2d');
    if (!ctx) return undefined;
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.65);
  } catch {
    return undefined;
  }
}

async function postJson<T>(url: string, body: Record<string, unknown>): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const rawText = await res.text();
    let data: ({ error?: string } & T) | null = null;
    try {
      data = rawText ? (JSON.parse(rawText) as { error?: string } & T) : null;
    } catch {
      data = null;
    }

    const looksLikeHtml = rawText.trimStart().startsWith('<');
    if (!res.ok) {
      if (data?.error) {
        throw new Error(data.error);
      }
      if (res.status === 404) {
        throw new Error('Scanner API endpoint not found (404). Make sure the app is running on Vercel or with `vercel dev`.');
      }
      if (res.status === 413) {
        throw new Error('Photo upload is too large for the server (413). Please try a smaller or cropped photo.');
      }
      throw new Error(`Server error (${res.status}). Please try again.`);
    }
    if (looksLikeHtml) {
      throw new Error(
        'Scanner API returned an HTML page instead of JSON. Deploy with Vercel serverless /api routes or run locally with `npm run dev` / `vercel dev`.'
      );
    }
    if (!data || typeof data !== 'object') {
      throw new Error('Unexpected empty response from server. Please try again.');
    }
    return data;
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error('Scan timed out. Check your internet connection and try again.');
    }
    if (err instanceof TypeError) {
      throw new Error('Network error. Please check your internet connection and try again.');
    }
    throw err instanceof Error ? err : new Error('Request failed. Please try again.');
  } finally {
    clearTimeout(timer);
  }
}

export interface ApiHealthStatus {
  ok: boolean;
  apiReachable: boolean;
  hasGeminiKey: boolean;
  liveVerified?: boolean;
  model?: string;
  message: string;
}

export const soilApi = {
  async checkHealth(probe = false): Promise<ApiHealthStatus> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15_000);
    try {
      const res = await fetch(`/api/health${probe ? '?probe=1' : ''}`, {
        method: 'GET',
        signal: controller.signal,
      });
      const rawText = await res.text();
      if (rawText.trimStart().startsWith('<')) {
        return {
          ok: false,
          apiReachable: false,
          hasGeminiKey: false,
          message: 'Scanner API routes (/api/*) are unavailable here. Deploy on Vercel or run with `npm run dev` / `vercel dev`.',
        };
      }
      const parsed = rawText ? (JSON.parse(rawText) as Partial<ApiHealthStatus>) : {};
      return {
        ok: Boolean(parsed.ok && res.ok),
        apiReachable: parsed.apiReachable ?? true,
        hasGeminiKey: Boolean(parsed.hasGeminiKey),
        liveVerified: parsed.liveVerified,
        model: parsed.model,
        message: parsed.message || (res.ok ? 'API connected.' : `API health check returned HTTP ${res.status}.`),
      };
    } catch {
      return {
        ok: false,
        apiReachable: false,
        hasGeminiKey: false,
        message: 'Could not reach /api/health. Check your internet connection or Vercel deployment.',
      };
    } finally {
      clearTimeout(timer);
    }
  },
  analyzeSoil(imageBase64: string, mimeType: string): Promise<SoilScanResult> {
    return postJson('/api/analyze-soil', { imageBase64, mimeType });
  },
  identifyCrop(imageBase64: string, mimeType: string): Promise<CropIdResult> {
    return postJson('/api/identify-crop', { imageBase64, mimeType });
  },
  analyzePlantHealth(imageBase64: string, mimeType: string, cropName: string): Promise<PlantHealthResult> {
    return postJson('/api/plant-health', { imageBase64, mimeType, cropName });
  },
  extractNpkReport(imageBase64: string, mimeType: string): Promise<NpkLabResult> {
    return postJson('/api/npk-extract', { imageBase64, mimeType });
  },
  fetchRegionalNpk(crop: string, region: string, soilType: string): Promise<NpkRegionalResult> {
    return postJson('/api/npk-regional', { crop, region, soilType });
  },
  generateAdvice(topic: string, question: string, contextSnapshot: string): Promise<AiAdvisoryPlan> {
    return postJson('/api/advisor', { topic, question, contextSnapshot });
  },
};
