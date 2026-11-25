import type { VercelRequest, VercelResponse } from '@vercel/node';
import {
  GEMINI_MODEL,
  Type,
  extractResponseText,
  handleGeminiError,
  normalizeString,
  normalizeStringArray,
  parseJsonSafe,
  parseRequestBody,
  parseValidatedImage,
  requirePostAndClient,
} from './_shared.js';

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
                'You are assisting FarmerBuddy (SMART SOIL), an AI-powered soil monitoring system.',
                'Analyze this soil photograph objectively in simple, clear, farmer-friendly language.',
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

    const parsed = parseJsonSafe(extractResponseText(response));
    const possibleSoilType = normalizeString(parsed.possibleSoilType, '');
    const visibleCharacteristics = normalizeString(parsed.visibleCharacteristics, '');
    if (!possibleSoilType || !visibleCharacteristics) {
      throw new Error('Incomplete soil analysis payload from Gemini.');
    }

    return res.status(200).json({
      visibleCharacteristics,
      possibleSoilType,
      organicMatterIndicators: normalizeString(
        parsed.organicMatterIndicators,
        'No clear organic matter indicators visible in photo.'
      ),
      uncertaintyExplanation: normalizeString(
        parsed.uncertaintyExplanation,
        'Visual scan cannot measure pH, moisture %, or NPK nutrients. Confirm with sensor or lab tests.'
      ),
      recommendedAdditionalTests: normalizeStringArray(parsed.recommendedAdditionalTests, [
        'Check root-zone moisture with calibrated ESP32 probe',
        'Run a laboratory Soil Health Card test for pH and NPK',
      ]),
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'Soil scan',
      err,
      'Unable to complete soil image analysis right now. Please try a clearer photo.'
    );
  }
}
