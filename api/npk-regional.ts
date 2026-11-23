import type { VercelRequest, VercelResponse } from '@vercel/node';
import { GEMINI_MODEL, Type, parseJsonSafe, requirePostAndClient } from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { crop, region, soilType } = (req.body ?? {}) as {
    crop?: unknown;
    region?: unknown;
    soilType?: unknown;
  };

  if (typeof crop !== 'string' || !crop.trim() || typeof region !== 'string' || !region.trim()) {
    return res.status(400).json({ error: 'Please provide both Crop and State/District/Region.' });
  }

  const cleanCrop = crop.trim().slice(0, 80);
  const cleanRegion = region.trim().slice(0, 120);
  const cleanSoil = typeof soilType === 'string' && soilType.trim() ? soilType.trim().slice(0, 80) : 'Not specified';

  try {
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: [
        {
          role: 'user',
          parts: [
            {
              text: [
                `Provide a cautious regional soil nutrient reference overview for Crop: ${cleanCrop}, Region: ${cleanRegion}, Soil Type: ${cleanSoil}.`,
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

    const parsed = parseJsonSafe(response.text);
    return res.status(200).json({
      crop: cleanCrop,
      region: cleanRegion,
      soilType: cleanSoil,
      ...parsed,
    });
  } catch (err) {
    console.error('Regional NPK lookup failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to fetch regional reference right now. Please try again.' });
  }
}
