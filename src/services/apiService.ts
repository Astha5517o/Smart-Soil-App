import type {
  AiAdvisoryPlan,
  CropIdResult,
  NpkLabResult,
  NpkRegionalResult,
  PlantHealthResult,
  SoilScanResult,
} from '../types/soil';

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

export async function compressImageFile(file: File, maxDim = 1024): Promise<{ base64: string; mimeType: string; previewUrl: string }> {
  if (!ALLOWED_TYPES.has(file.type)) {
    throw new Error('Please choose a JPEG, PNG, or WebP image file.');
  }
  if (file.size > 15 * 1024 * 1024) {
    throw new Error('Selected file is larger than 15MB.');
  }

  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error('Could not read image file.'));
    reader.readAsDataURL(file);
  });

  const img = await new Promise<HTMLImageElement>((resolve, reject) => {
    const el = new Image();
    el.onload = () => resolve(el);
    el.onerror = () => reject(new Error('Invalid image file.'));
    el.src = dataUrl;
  });

  const scale = Math.min(maxDim / img.width, maxDim / img.height, 1);
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(img.width * scale));
  canvas.height = Math.max(1, Math.round(img.height * scale));
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas compression unavailable.');
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

  const compressedUrl = canvas.toDataURL('image/jpeg', 0.84);
  return {
    base64: compressedUrl,
    mimeType: 'image/jpeg',
    previewUrl: compressedUrl,
  };
}

async function postJson<T>(url: string, body: Record<string, unknown>): Promise<T> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = (await res.json().catch(() => ({}))) as { error?: string } & T;
  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }
  return data;
}

export const soilApi = {
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
