import type { VercelRequest, VercelResponse } from '@vercel/node';
import {
  GEMINI_MODEL,
  Type,
  extractResponseText,
  handleGeminiError,
  normalizeString,
  parseJsonSafe,
  parseRequestBody,
  requirePostAndClient,
} from './_shared.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const ai = requirePostAndClient(req, res);
  if (!ai) return;

  const { topic, question, contextSnapshot } = parseRequestBody(req);

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
                'You are the FarmerBuddy (SMART SOIL) Agronomy Advisor.',
                `Focus Topic: ${safeTopic}`,
                `Question: ${safeQuestion}`,
                '',
                'AVAILABLE CONTEXT:',
                contextSnapshot.slice(0, 2500),
                '',
                'STRICT SCIENTIFIC RULES:',
                '- Distinguish DEMO data and REGIONAL REFERENCE ESTIMATES from LIVE ESP32 readings and VERIFIED LAB REPORTS.',
                '- Do NOT automatically recommend soil amendments from a single pH reading alone.',
                '- Consider locally relevant organic materials (compost, farmyard manure, organic mulch, crop residues) only when appropriate.',
                '- NEVER invent application quantities, guaranteed outcomes, or fake scientific citations.',
                '- Write clearly in simple, farmer-friendly English.',
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

    const parsed = parseJsonSafe(extractResponseText(response));
    const summary = normalizeString(parsed.summary, '');
    const rawRecs = Array.isArray(parsed.recommendations) ? parsed.recommendations : [];
    const recommendations = rawRecs
      .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object' && !Array.isArray(item))
      .map((item) => ({
        category: normalizeString(item.category, 'Soil Guidance'),
        identifiedIssue: normalizeString(item.identifiedIssue, 'Condition review'),
        whyItMatters: normalizeString(item.whyItMatters, 'Supports balanced crop growth.'),
        suggestedNextStep: normalizeString(item.suggestedNextStep, 'Verify field conditions before acting.'),
        supportingEvidence: normalizeString(item.supportingEvidence, 'Current sensor and crop context.'),
        limitationsAndUncertainty: normalizeString(
          item.limitationsAndUncertainty,
          'Confirm with live probe measurements and a laboratory Soil Health Card.'
        ),
      }));

    if (!summary || recommendations.length === 0) {
      throw new Error('Incomplete advisory payload from Gemini.');
    }

    return res.status(200).json({
      summary,
      recommendations,
      contextSnapshot: contextSnapshot.slice(0, 2500),
      isDemo: false,
    });
  } catch (err) {
    return handleGeminiError(
      res,
      'AI advisor',
      err,
      'Unable to generate AI advice right now. Please try again.'
    );
  }
}
