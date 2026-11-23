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
                'You are assisting SMART SOIL, a school science exhibition soil monitoring system.',
                'Analyze this soil photograph objectively.',
                'STRICT RULES:',
                '- Describe only visible characteristics (color, clodding, texture clues, visible organic residue).',
                '- Estimate a possible broad soil type cautiously.',
                '- NEVER claim that an image can measure soil pH, soil moisture percentage, or NPK nutrients.',
                '- Explain uncertainty clearly and suggest practical additional tests.',
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
            visibleCharacteristics: { type: Type.STRING },
            possibleSoilType: { type: Type.STRING },
            organicMatterIndicators: { type: Type.STRING },
            uncertaintyExplanation: { type: Type.STRING },
            recommendedAdditionalTests: {
              type: Type.ARRAY,
              items: { type: Type.STRING },
            },
          },
          required: [
            'visibleCharacteristics',
            'possibleSoilType',
            'organicMatterIndicators',
            'uncertaintyExplanation',
            'recommendedAdditionalTests',
          ],
        },
      },
    });

    return res.status(200).json(parseJsonSafe(response.text));
  } catch (err) {
    console.error('Soil scan failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to complete soil image analysis right now. Please try again.' });
  }
}
