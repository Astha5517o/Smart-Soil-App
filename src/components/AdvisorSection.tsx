import React, { useState } from 'react';
import { soilApi } from '../services/apiService';
import type { AiAdvisoryPlan, SoilRecord } from '../types/soil';

const TOPICS = [
  'Full Soil Plan',
  'Irrigation Guidance',
  'pH Management',
  'Natural & Organic Amendments',
  'Crop Nutrient Management',
  'Indigenous Practices',
];

interface AdvisorSectionProps {
  contextSnapshot: string;
  cropName: string;
  onSaveRecord: (record: Omit<SoilRecord, 'id' | 'timestampMs'>) => void;
}

export const AdvisorSection: React.FC<AdvisorSectionProps> = ({
  contextSnapshot,
  cropName,
  onSaveRecord,
}) => {
  const [topic, setTopic] = useState(TOPICS[0]);
  const [question, setQuestion] = useState('');
  const [showContext, setShowContext] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [plan, setPlan] = useState<AiAdvisoryPlan | null>(null);
  const [saved, setSaved] = useState(false);

  const handleGenerate = async () => {
    setError(null);
    setLoading(true);
    setSaved(false);
    try {
      const result = await soilApi.generateAdvice(topic, question, contextSnapshot);
      setPlan(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to reach AI Advisor.');
    } finally {
      setLoading(false);
    }
  };

  const handleLoadDemo = () => {
    setError(null);
    setSaved(false);
    setPlan({
      summary: `DEMO ADVISORY: Guidance based on current display settings for ${cropName}. Confirm with live ESP32 measurements and a Soil Health Card before making field changes.`,
      contextSnapshot,
      isDemo: true,
      recommendations: [
        {
          category: 'Irrigation & Moisture',
          identifiedIssue: 'Evaluating soil moisture against crop threshold.',
          whyItMatters: 'Balanced root-zone moisture supports nutrient uptake without waterlogging roots.',
          suggestedNextStep: 'Verify probe readings in two spots of the plot and apply organic mulch to conserve moisture.',
          supportingEvidence: 'Calibrated capacitive sensor trend and DS18B20 temperature probe.',
          limitationsAndUncertainty: 'DEMO OUTPUT: Capacitive readings depend on soil compaction and dry/wet calibration.',
        },
        {
          category: 'Organic & pH Management',
          identifiedIssue: 'Avoiding premature amendments from a single pH probe check.',
          whyItMatters: 'Soil buffering capacity varies by clay and organic matter; unverified lime or sulfur applications can harm crops.',
          suggestedNextStep: 'Incorporate well-decomposed farmyard manure or compost between cycles and confirm pH with a lab test.',
          supportingEvidence: 'Standard organic matter buffering practices.',
          limitationsAndUncertainty: 'DEMO OUTPUT: Exact amendment quantities require a laboratory Soil Health Card.',
        },
      ],
    });
  };

  return (
    <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
      <div>
        <h1 className="font-headline text-headline-lg text-light-on-surface">AI Soil Advisor</h1>
        <p className="font-body text-body-md text-light-on-surface-variant">
          Cautious agronomy guidance combining ESP32 probe readings, scans, and NPK records.
        </p>
      </div>

      <div className="bg-light-surface-container-low rounded-lg p-gutter-md">
        <button
          type="button"
          onClick={() => setShowContext(!showContext)}
          className="w-full flex items-center justify-between font-label text-label-lg text-light-on-surface"
        >
          <span>Active Context Snapshot</span>
          <span className="text-light-primary">{showContext ? 'Hide' : 'Inspect'}</span>
        </button>
        {showContext && (
          <pre className="mt-2 font-mono text-[12px] text-light-on-surface-variant whitespace-pre-wrap">
            {contextSnapshot}
          </pre>
        )}
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {TOPICS.map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTopic(t)}
            className={`px-3.5 py-2 rounded-full font-label text-label-md shrink-0 ${
              topic === t
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
        <textarea
          rows={3}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a soil or crop question (optional)..."
          className="w-full rounded-sm border-light-outline-variant bg-light-surface-bright font-body text-body-md"
        />
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={loading}
            onClick={() => void handleGenerate()}
            className="flex-1 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
          >
            {loading ? 'Generating Advice...' : 'Generate Guided Advice'}
          </button>
          <button
            type="button"
            onClick={handleLoadDemo}
            className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface-variant font-label text-label-md"
          >
            Load Demo Advice
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-light-error-container text-light-on-error-container rounded-lg p-gutter-md font-body text-body-sm">
          {error}
        </div>
      )}

      {plan && (
        <div className="flex flex-col gap-gutter-md">
          <div className="bg-light-primary-container text-light-on-primary-container rounded-lg p-gutter-md">
            <div className="flex justify-between items-center mb-1">
              <span className="font-label text-label-sm uppercase">Advisory Summary</span>
              <span
                className={`px-2.5 py-0.5 rounded-full font-label text-label-sm ${
                  plan.isDemo ? 'bg-warning-container text-on-warning-container' : 'bg-success-container text-on-success-container'
                }`}
              >
                {plan.isDemo ? 'DEMO ADVICE' : 'AI GENERATED'}
              </span>
            </div>
            <p className="font-body text-body-md">{plan.summary}</p>
          </div>

          {plan.recommendations.map((rec, idx) => (
            <article key={idx} className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-1.5">
              <h2 className="font-headline text-title-md text-light-primary">
                {idx + 1}. {rec.category}
              </h2>
              <p className="font-body text-body-sm"><strong>1. Identified Issue:</strong> {rec.identifiedIssue}</p>
              <p className="font-body text-body-sm"><strong>2. Why It Matters:</strong> {rec.whyItMatters}</p>
              <p className="font-body text-body-sm"><strong>3. Suggested Next Step:</strong> {rec.suggestedNextStep}</p>
              <p className="font-body text-body-sm"><strong>4. Supporting Evidence:</strong> {rec.supportingEvidence}</p>
              <p className="font-body text-body-sm"><strong>5. Limitations &amp; Uncertainty:</strong> {rec.limitationsAndUncertainty}</p>
            </article>
          ))}

          <button
            type="button"
            disabled={saved}
            onClick={() => {
              onSaveRecord({
                category: 'AI_RECOMMENDATION',
                origin: plan.isDemo ? 'DEMO_DATA' : 'AI_INTERPRETATION',
                isDemo: Boolean(plan.isDemo),
                dataSource: plan.isDemo ? 'Demo AI Advisor' : 'Gemini Soil Advisor',
                title: `${plan.isDemo ? 'Demo ' : ''}AI Soil Plan — ${cropName}`,
                summary: plan.summary,
                fullDetails: `${plan.summary}\n\n${plan.recommendations
                  .map(
                    (r, i) =>
                      `${i + 1}. ${r.category}\nIssue: ${r.identifiedIssue}\nWhy: ${r.whyItMatters}\nNext: ${r.suggestedNextStep}\nEvidence: ${r.supportingEvidence}\nLimitations: ${r.limitationsAndUncertainty}`
                  )
                  .join('\n\n')}`,
              });
              setSaved(true);
            }}
            className="min-h-[48px] px-5 rounded-full border border-light-outline-variant bg-light-surface-container-lowest font-label text-label-lg disabled:opacity-60"
          >
            {saved ? 'Saved to Reports' : 'Save AI Plan to Reports'}
          </button>
        </div>
      )}
    </main>
  );
};
