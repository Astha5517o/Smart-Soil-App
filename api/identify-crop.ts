import type { VercelRequest, VercelResponse } from '@vercel/node';
import {
  GEMINI_MODEL,
  Type,
  extractResponseText,
  handleGeminiError,
  normalizeString,
  parseJsonSafe,
  parseRequestBody,
  parseValidatedImage,
  requirePostAndClient,
} from './_shared.js';

const VALID_STAGES = new Set(['Seedling', 'Vegetative', 'Flowering / Reproductive', 'Maturity']);

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { imageBase64, mimeType } = parseRequestBody(req);
  const img = parseValidatedImage(imageBase64, mimeType);
  if ('error' in img) {
    return res.status(400).json({ error: img.error });
  }

  try {
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: [
        {
          role: 'user',
          parts: [
            { inlineData: { data: img.data, mimeType: img.mimeType } },
            {
              text: [
                'Identify the crop in this photo for FarmerBuddy (SMART SOIL) in simple, clear language.',
                'Explain visible identifying features (leaf shape, venation, stem, panicle/fruit).',
                'State confidence level (High, Moderate, or Low) and uncertainty/look-alikes clearly so the farmer can confirm before saving.',
                'Use suggestedGrowthStage strictly from: Seedling, Vegetative, Flowering / Reproductive, Maturity.',
              ].join('\n'),
            },
          ],
        },
      ],
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            possibleCropName: { type: Type.STRING },
            confidenceLevel: { type: Type.STRING },
            identifyingFeatures: { type: Type.STRING },
            uncertaintyNotes: { type: Type.STRING },
            suggestedGrowthStage: { type: Type.STRING },
          },
          required: [
            'possibleCropName',
            'confidenceLevel',
            'identifyingFeatures',
            'uncertaintyNotes',
            'suggestedGrowthStage',
          ],
        },
      },
    });

    const parsed = parseJsonSafe(extractResponseText(response));
    const possibleCropName = normalizeString(parsed.possibleCropName, '');
    if (!possibleCropName) {
      throw new Error('Incomplete crop identification payload from Gemini.');
    }

    const rawStage = normalizeString(parsed.suggestedGrowthStage, 'Vegetative');
    const suggestedGrowthStage = VALID_STAGES.has(rawStage) ? rawStage : 'Vegetative';

    return res.status(200).json({
      possibleCropName,
      confidenceLevel: normalizeString(parsed.confidenceLevel, 'Moderate'),
      identifyingFeatures: normalizeString(parsed.identifyingFeatures, 'Visible leaf and stem pattern evaluated.'),
      uncertaintyNotes: normalizeString(
        parsed.uncertaintyNotes,
        'Visual identification may confuse similar crops. Please review and confirm before saving.'
      ),
      suggestedGrowthStage,
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'Crop ID',
      err,
      'Unable to identify crop right now. Please try a clearer leaf or plant photo.'
    );
  }
}
