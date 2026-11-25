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

  const { imageBase64, mimeType, cropName } = parseRequestBody(req);
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
                `Examine this plant or leaf photograph for FarmerBuddy (current crop: ${safeCrop}).`,
                'Use simple, clear, farmer-friendly wording.',
                'STRICT RULES:',
                '- Describe visible symptoms objectively.',
                '- List multiple possible explanations without claiming a definitive disease diagnosis from an image alone.',
                '- Provide practical field checks and cautious next steps without prescribing unverified chemical dosages.',
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

    const parsed = parseJsonSafe(extractResponseText(response));
    const visibleSymptoms = normalizeString(parsed.visibleSymptoms, '');
    if (!visibleSymptoms) {
      throw new Error('Incomplete plant health payload from Gemini.');
    }

    return res.status(200).json({
      visibleSymptoms,
      possibleExplanations: normalizeStringArray(parsed.possibleExplanations, [
        'Multiple factors (water, nutrients, or early pest/leaf stress) can look similar.',
      ]),
      suggestedChecks: normalizeStringArray(parsed.suggestedChecks, [
        'Inspect underside of leaves and check soil moisture in the root zone.',
      ]),
      cautiousNextSteps: normalizeStringArray(parsed.cautiousNextSteps, [
        'Consult a local agriculture officer or KVK before spraying treatments.',
      ]),
      diagnosisLimitations: normalizeString(
        parsed.diagnosisLimitations,
        'A photo alone cannot confirm a plant disease or exact nutrient deficiency.'
      ),
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'Plant health scan',
      err,
      'Unable to analyze plant health right now. Please try a clearer close-up photo.'
    );
  }
}
