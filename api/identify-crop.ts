import type { VercelRequest, VercelResponse } from '@vercel/node';
import { GEMINI_MODEL, Type, parseJsonSafe, parseValidatedImage, requirePostAndClient } from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { imageBase64, mimeType } = (req.body ?? {}) as { imageBase64?: unknown; mimeType?: unknown };
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
                'Identify the crop in this photo for SMART SOIL.',
                'Explain visible identifying features (leaf shape, venation, stem, panicle/fruit).',
                'State confidence level and uncertainty/look-alikes clearly so the user can confirm before saving.',
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

    return res.status(200).json(parseJsonSafe(response.text));
  } catch (err) {
    console.error('Crop ID failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to identify crop right now. Please try again.' });
  }
}
