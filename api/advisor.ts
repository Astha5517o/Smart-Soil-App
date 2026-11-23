import type { VercelRequest, VercelResponse } from '@vercel/node';
import { GEMINI_MODEL, Type, parseJsonSafe, requirePostAndClient } from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { topic, question, contextSnapshot } = (req.body ?? {}) as {
    topic?: unknown;
    question?: unknown;
    contextSnapshot?: unknown;
  };

  if (typeof contextSnapshot !== 'string' || !contextSnapshot.trim()) {
    return res.status(400).json({ error: 'Missing sensor and crop context snapshot.' });
  }

  const safeTopic = typeof topic === 'string' && topic.trim() ? topic.trim().slice(0, 80) : 'Full Soil Plan';
  const safeQuestion =
    typeof question === 'string' && question.trim()
      ? question.trim().slice(0, 400)
      : 'Provide practical soil and crop guidance based on current readings and context.';

  try {
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: [
        {
          role: 'user',
          parts: [
            {
              text: [
                'You are the SMART SOIL Agronomy Advisor for a school science exhibition.',
                `Focus Topic: ${safeTopic}`,
                `Question: ${safeQuestion}`,
                '',
                'AVAILABLE CONTEXT:',
                contextSnapshot.slice(0, 2500),
                '',
                'STRICT SCIENTIFIC RULES:',
                '- Distinguish DEMO data from LIVE ESP32 readings.',
                '- Do NOT automatically recommend soil amendments from a single pH reading alone.',
                '- Consider locally relevant organic materials (compost, farmyard manure, organic mulch, crop residues) only when appropriate.',
                '- NEVER invent application quantities, guaranteed outcomes, or fake scientific citations.',
                '- Every recommendation item MUST include: category, identifiedIssue, whyItMatters, suggestedNextStep, supportingEvidence, limitationsAndUncertainty.',
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
            summary: { type: Type.STRING },
            recommendations: {
              type: Type.ARRAY,
              items: {
                type: Type.OBJECT,
                properties: {
                  category: { type: Type.STRING },
                  identifiedIssue: { type: Type.STRING },
                  whyItMatters: { type: Type.STRING },
                  suggestedNextStep: { type: Type.STRING },
                  supportingEvidence: { type: Type.STRING },
                  limitationsAndUncertainty: { type: Type.STRING },
                },
                required: [
                  'category',
                  'identifiedIssue',
                  'whyItMatters',
                  'suggestedNextStep',
                  'supportingEvidence',
                  'limitationsAndUncertainty',
                ],
              },
            },
          },
          required: ['summary', 'recommendations'],
        },
      },
    });

    const parsed = parseJsonSafe(response.text);
    return res.status(200).json({
      ...parsed,
      contextSnapshot: contextSnapshot.slice(0, 2500),
      isDemo: false,
    });
  } catch (err) {
    console.error('AI advisor failed:', err instanceof Error ? err.message : 'Unknown error');
    return res.status(502).json({ error: 'Unable to generate AI advice right now. Please try again.' });
  }
}
