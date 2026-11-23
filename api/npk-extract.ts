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
                'Extract readable Nitrogen (N), Phosphorus (P), and Potassium (K) values from this uploaded Soil Health Card or laboratory soil report.',
                'STRICT RULES:',
                "- Extract ONLY values printed on the document. If blurred or missing, output 'Not readable'.",
                '- NEVER invent or guess missing lab numbers.',
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
            nitrogen: { type: Type.STRING },
            phosphorus: { type: Type.STRING },
            potassium: { type: Type.STRING },
            unit: { type: Type.STRING },
            labSource: { type: Type.STRING },
            reportDate: { type: Type.STRING },
            extractionNotes: { type: Type.STRING },
          },
          required: ['nitrogen', 'phosphorus', 'potassium', 'unit', 'labSource', 'reportDate', 'extractionNotes'],
        },
      },
    });

    return res.status(200).json(parseJsonSafe(response.text));
  } catch (err) {
    console.error('NPK extraction failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to extract NPK report values right now. Please try again.' });
  }
}
