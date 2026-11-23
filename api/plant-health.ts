import type { VercelRequest, VercelResponse } from '@vercel/node';
import { GEMINI_MODEL, Type, parseJsonSafe, parseValidatedImage, requirePostAndClient } from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { imageBase64, mimeType, cropName } = (req.body ?? {}) as {
    imageBase64?: unknown;
    mimeType?: unknown;
    cropName?: unknown;
  };
  const img = parseValidatedImage(imageBase64, mimeType);
  if ('error' in img) {
    return res.status(400).json({ error: img.error });
  }
  const safeCrop = typeof cropName === 'string' && cropName.trim() ? cropName.trim().slice(0, 80) : 'Crop';

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
                `Examine this plant or leaf photograph (current crop: ${safeCrop}).`,
                'STRICT RULES:',
                '- Describe visible symptoms objectively.',
                '- List multiple possible explanations without claiming a definitive disease diagnosis from an image alone.',
                '- Provide practical field checks and cautious next steps.',
                '- State the limitations of image-based diagnosis clearly.',
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
            visibleSymptoms: { type: Type.STRING },
            possibleExplanations: { type: Type.ARRAY, items: { type: Type.STRING } },
            suggestedChecks: { type: Type.ARRAY, items: { type: Type.STRING } },
            cautiousNextSteps: { type: Type.ARRAY, items: { type: Type.STRING } },
            diagnosisLimitations: { type: Type.STRING },
          },
          required: [
            'visibleSymptoms',
            'possibleExplanations',
            'suggestedChecks',
            'cautiousNextSteps',
            'diagnosisLimitations',
          ],
        },
      },
    });

    return res.status(200).json(parseJsonSafe(response.text));
  } catch (err) {
    console.error('Plant health scan failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to analyze plant health right now. Please try again.' });
  }
}
