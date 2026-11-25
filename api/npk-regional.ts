import type { VercelRequest, VercelResponse } from '@vercel/node';
import {
  Type,
  extractResponseText,
  generateWithModelFallback,
  handleGeminiError,
  normalizeString,
  parseJsonSafe,
  parseRequestBody,
  requirePostAndClient,
} from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { crop, region, soilType } = parseRequestBody(req);

  if (typeof crop !== 'string' || !crop.trim() || typeof region !== 'string' || !region.trim()) {
    return res.status(400).json({ error: 'Please provide both Crop and State/District/Region.' });
  }

  const cleanCrop = crop.trim().slice(0, 80);
  const cleanRegion = region.trim().slice(0, 120);
  const cleanSoil = typeof soilType === 'string' && soilType.trim() ? soilType.trim().slice(0, 80) : 'Not specified';

  try {
    const response = await generateWithModelFallback(ai, {
      contents: [
        {
          role: 'user',
          parts: [
            {
              text: [
                `Provide a cautious regional soil nutrient reference overview for FarmerBuddy — Crop: ${cleanCrop}, Region: ${cleanRegion}, Soil Type: ${cleanSoil}.`,
                'STRICT SCIENTIFIC RULES:',
                '- NEVER invent numerical field NPK values.',
                '- NEVER present crop nutrient requirements as measured soil nutrient levels.',
                '- Set isInsufficientInfo = true if reliable region-specific reference data is lacking.',
                '- Cite only general established agricultural extension references without fabricating fake URLs or report IDs.',
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
            sufficiencyStatus: { type: Type.STRING },
            nutrientOverview: { type: Type.STRING },
            source: { type: Type.STRING },
            referenceDate: { type: Type.STRING },
            limitations: { type: Type.STRING },
            isInsufficientInfo: { type: Type.BOOLEAN },
          },
          required: [
            'sufficiencyStatus',
            'nutrientOverview',
            'source',
            'referenceDate',
            'limitations',
            'isInsufficientInfo',
          ],
        },
      },
    });

    const parsed = parseJsonSafe(extractResponseText(response));
    const nutrientOverview = normalizeString(parsed.nutrientOverview, '');
    if (!nutrientOverview) {
      throw new Error('Incomplete regional NPK payload from Gemini.');
    }

    return res.status(200).json({
      crop: cleanCrop,
      region: cleanRegion,
      soilType: cleanSoil,
      sufficiencyStatus: normalizeString(parsed.sufficiencyStatus, 'Regional reference overview (not a field measurement)'),
      nutrientOverview,
      source: normalizeString(parsed.source, 'General Agricultural Extension Reference'),
      referenceDate: normalizeString(parsed.referenceDate, 'General Reference'),
      limitations: normalizeString(
        parsed.limitations,
        'Regional averages vary widely by farm plot; confirm with a laboratory Soil Health Card test.'
      ),
      isInsufficientInfo: Boolean(parsed.isInsufficientInfo),
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'Regional NPK lookup',
      err,
      'Unable to fetch regional reference right now. Please try again.'
    );
  }
}
