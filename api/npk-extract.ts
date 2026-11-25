import type { VercelRequest, VercelResponse } from '@vercel/node';
import {
  Type,
  extractResponseText,
  generateWithModelFallback,
  handleGeminiError,
  isUnreadableToken,
  normalizeString,
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
    const response = await generateWithModelFallback(ai, {
      contents: [
        {
          role: 'user',
          parts: [
            { inlineData: { data: img.data, mimeType: img.mimeType } },
            {
              text: [
                'Extract readable Nitrogen (N), Phosphorus (P), and Potassium (K) values from this uploaded Soil Health Card or laboratory soil report for FarmerBuddy.',
                'STRICT RULES:',
                "- Extract ONLY values clearly printed on the document. If blurred, missing, or uncertain, output 'Not readable'.",
                '- NEVER invent or guess missing lab numbers.',
                '- Clearly note any unreadable or missing fields in extractionNotes.',
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

    const parsed = parseJsonSafe(extractResponseText(response));
    const nitrogen = normalizeString(parsed.nitrogen, 'Not readable');
    const phosphorus = normalizeString(parsed.phosphorus, 'Not readable');
    const potassium = normalizeString(parsed.potassium, 'Not readable');
    const isUnreadable =
      isUnreadableToken(nitrogen) && isUnreadableToken(phosphorus) && isUnreadableToken(potassium);

    return res.status(200).json({
      nitrogen,
      phosphorus,
      potassium,
      unit: normalizeString(parsed.unit, 'Not readable'),
      labSource: normalizeString(parsed.labSource, 'Not readable'),
      reportDate: normalizeString(parsed.reportDate, 'Not readable'),
      extractionNotes: normalizeString(
        parsed.extractionNotes,
        isUnreadable
          ? 'Could not clearly read N, P, K values from this image. Please upload a sharper report photo or enter values manually.'
          : 'Verify extracted values against your printed Soil Health Card before saving.'
      ),
      isUnreadable,
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'NPK extraction',
      err,
      'Unable to read NPK report values right now. Please upload a clearer photo.'
    );
  }
}
