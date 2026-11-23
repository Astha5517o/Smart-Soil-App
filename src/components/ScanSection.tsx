import React, { useState } from 'react';
import { compressImageFile, soilApi } from '../services/apiService';
import {
  GROWTH_STAGES,
  type CropIdResult,
  type NpkLabResult,
  type NpkRegionalResult,
  type PlantHealthResult,
  type SoilRecord,
  type SoilScanResult,
} from '../types/soil';

export type ScanSubView = 'hub' | 'soil' | 'crop' | 'plant' | 'npk';

interface ScanSectionProps {
  activeSubView: ScanSubView;
  cropName: string;
  onChangeSubView: (view: ScanSubView) => void;
  onConfirmCrop: (cropName: string, growthStage: string) => void;
  onSaveRecord: (record: Omit<SoilRecord, 'id' | 'timestampMs'>) => void;
}

export const ScanSection: React.FC<ScanSectionProps> = ({
  activeSubView,
  cropName,
  onChangeSubView,
  onConfirmCrop,
  onSaveRecord,
}) => {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [soilRes, setSoilRes] = useState<SoilScanResult | null>(null);
  const [cropRes, setCropRes] = useState<CropIdResult | null>(null);
  const [confirmedCrop, setConfirmedCrop] = useState('');
  const [confirmedStage, setConfirmedStage] = useState<string>(GROWTH_STAGES[1]);
  const [cropSaved, setCropSaved] = useState(false);

  const [plantRes, setPlantRes] = useState<PlantHealthResult | null>(null);

  const [npkMethod, setNpkMethod] = useState<'lab' | 'regional'>('lab');
  const [labRes, setLabRes] = useState<NpkLabResult | null>(null);
  const [labSaved, setLabSaved] = useState(false);

  const [regCrop, setRegCrop] = useState(cropName);
  const [regLocation, setRegLocation] = useState('');
  const [regSoilType, setRegSoilType] = useState('');
  const [regRes, setRegRes] = useState<NpkRegionalResult | null>(null);

  const resetResults = () => {
    setError(null);
    setPreviewUrl(null);
  };

  const handleImageUpload = async (file: File, kind: 'soil' | 'crop' | 'plant' | 'npk') => {
    setError(null);
    setLoading(true);
    try {
      const { base64, mimeType, previewUrl: url } = await compressImageFile(file);
      setPreviewUrl(url);

      if (kind === 'soil') {
        const res = await soilApi.analyzeSoil(base64, mimeType);
        setSoilRes(res);
        onSaveRecord({
          category: 'SOIL_SCAN',
          origin: 'AI_INTERPRETATION',
          isDemo: false,
          dataSource: 'Gemini Vision Soil Scan',
          title: `Soil Scan — ${res.possibleSoilType}`,
          summary: res.visibleCharacteristics,
          fullDetails: `Visible: ${res.visibleCharacteristics}\nPossible type: ${res.possibleSoilType}\nOrganic matter: ${res.organicMatterIndicators}\nUncertainty: ${res.uncertaintyExplanation}\nTests: ${res.recommendedAdditionalTests.join('; ')}`,
          imageDataUrl: url,
        });
      } else if (kind === 'crop') {
        const res = await soilApi.identifyCrop(base64, mimeType);
        setCropRes(res);
        setConfirmedCrop(res.possibleCropName);
        setConfirmedStage(res.suggestedGrowthStage || GROWTH_STAGES[1]);
        setCropSaved(false);
      } else if (kind === 'plant') {
        const res = await soilApi.analyzePlantHealth(base64, mimeType, cropName);
        setPlantRes(res);
        onSaveRecord({
          category: 'PLANT_HEALTH_SCAN',
          origin: 'AI_INTERPRETATION',
          isDemo: false,
          dataSource: 'Gemini Vision Plant Health Check',
          title: `Plant Health Check — ${cropName}`,
          summary: res.visibleSymptoms,
          fullDetails: `Symptoms: ${res.visibleSymptoms}\nExplanations: ${res.possibleExplanations.join('; ')}\nChecks: ${res.suggestedChecks.join('; ')}\nNext steps: ${res.cautiousNextSteps.join('; ')}\nLimitations: ${res.diagnosisLimitations}`,
          imageDataUrl: url,
        });
      } else {
        const res = await soilApi.extractNpkReport(base64, mimeType);
        setLabRes(res);
        setLabSaved(false);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Analysis failed.');
    } finally {
      setLoading(false);
    }
  };

  if (activeSubView === 'hub') {
    return (
      <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
        <div>
          <h1 className="font-headline text-headline-lg text-light-on-surface">Scanning System</h1>
          <p className="font-body text-body-md text-light-on-surface-variant">
            Select a scan mode. Active crop: <strong className="text-light-on-surface">{cropName}</strong>
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter-md">
          {[
            {
              key: 'soil' as const,
              title: 'A. Soil Scan',
              badge: 'Gemini Vision',
              icon: 'terrain',
              bg: 'bg-soil-container text-on-soil-container',
              desc: 'Upload or capture a soil photo to describe visible traits, possible soil type, uncertainty, and follow-up tests. Never claims to measure pH or NPK.',
            },
            {
              key: 'crop' as const,
              title: 'B. Crop Identification',
              badge: 'Confirm Crop',
              icon: 'eco',
              bg: 'bg-light-secondary-container text-light-on-secondary-container',
              desc: 'Identify a crop with visible features and confidence notes. Requires your confirmation before saving.',
            },
            {
              key: 'plant' as const,
              title: 'C. Plant Health Scan',
              badge: 'Cautious Check',
              icon: 'healing',
              bg: 'bg-warning-container text-on-warning-container',
              desc: 'Inspect visible leaf symptoms, possible causes, and safe field checks without claiming a definitive diagnosis.',
            },
            {
              key: 'npk' as const,
              title: 'D. NPK Report',
              badge: 'Lab or Regional',
              icon: 'biotech',
              bg: 'bg-light-primary text-light-on-primary',
              desc: 'Upload & edit a Soil Health Card lab report, or look up a clearly labeled regional reference estimate.',
            },
          ].map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => {
                resetResults();
                onChangeSubView(item.key);
              }}
              className="text-left bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex items-start gap-gutter-md"
            >
              <span className={`w-12 h-12 rounded flex items-center justify-center shrink-0 ${item.bg}`}>
                <span className="material-symbols-outlined text-[26px]" aria-hidden="true">{item.icon}</span>
              </span>
              <div className="flex-1">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-headline text-title-lg text-light-on-surface">{item.title}</span>
                  <span className="px-2.5 py-0.5 rounded-full bg-light-surface-container text-light-on-surface-variant font-label text-label-sm">
                    {item.badge}
                  </span>
                </div>
                <p className="font-body text-body-sm text-light-on-surface-variant mt-1">{item.desc}</p>
              </div>
            </button>
          ))}
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
      <div className="flex items-center gap-gutter-sm">
        <button
          type="button"
          onClick={() => onChangeSubView('hub')}
          className="min-h-[44px] min-w-[44px] rounded-full bg-light-surface-container flex items-center justify-center text-light-on-surface"
          aria-label="Back to Scan Hub"
        >
          <span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>
        </button>
        <h1 className="font-headline text-headline-md text-light-on-surface">
          {activeSubView === 'soil' && 'A. Soil Scan'}
          {activeSubView === 'crop' && 'B. Crop Identification'}
          {activeSubView === 'plant' && 'C. Plant Health Scan'}
          {activeSubView === 'npk' && 'D. NPK Report'}
        </h1>
      </div>

      {activeSubView === 'npk' && (
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setNpkMethod('lab')}
            className={`flex-1 min-h-[44px] rounded-full font-label text-label-md ${
              npkMethod === 'lab' ? 'bg-light-primary text-light-on-primary clay-btn-primary' : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            1. Upload Lab Report
          </button>
          <button
            type="button"
            onClick={() => setNpkMethod('regional')}
            className={`flex-1 min-h-[44px] rounded-full font-label text-label-md ${
              npkMethod === 'regional' ? 'bg-light-primary text-light-on-primary clay-btn-primary' : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            2. Regional Reference
          </button>
        </div>
      )}

      {/* Upload Bar (for Soil, Crop, Plant, or NPK Lab) */}
      {(activeSubView !== 'npk' || npkMethod === 'lab') && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          {previewUrl && (
            <img src={previewUrl} alt="Uploaded preview" className="w-full max-h-64 object-cover rounded" />
          )}
          <div className="flex flex-wrap gap-gutter-sm">
            <label className="flex-1 min-h-[48px] px-4 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg flex items-center justify-center gap-2 cursor-pointer">
              <span className="material-symbols-outlined text-[20px]" aria-hidden="true">upload_file</span>
              Upload / Capture Photo
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                capture="environment"
                className="sr-only"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) void handleImageUpload(f, activeSubView === 'npk' ? 'npk' : activeSubView);
                  e.target.value = '';
                }}
              />
            </label>

            <button
              type="button"
              onClick={() => {
                setError(null);
                if (activeSubView === 'soil') {
                  const demo: SoilScanResult = {
                    visibleCharacteristics: 'Dark brown crumbly topsoil aggregates with visible fine root residue.',
                    possibleSoilType: 'Alluvial Clay Loam (visual estimate)',
                    organicMatterIndicators: 'Moderate humus darkening in top layer.',
                    uncertaintyExplanation: 'DEMO SAMPLE: Images cannot measure pH, moisture %, or NPK nutrients.',
                    recommendedAdditionalTests: ['Verify moisture with calibrated HW-390 probe', 'Run Soil Health Card NPK lab test'],
                    isDemo: true,
                  };
                  setSoilRes(demo);
                  onSaveRecord({
                    category: 'SOIL_SCAN',
                    origin: 'DEMO_DATA',
                    isDemo: true,
                    dataSource: 'Demo Soil Scan Sample',
                    title: 'Demo Soil Scan — Alluvial Clay Loam',
                    summary: demo.visibleCharacteristics,
                    fullDetails: `${demo.visibleCharacteristics}\n${demo.uncertaintyExplanation}`,
                  });
                } else if (activeSubView === 'crop') {
                  const demo: CropIdResult = {
                    possibleCropName: 'Paddy (Rice)',
                    confidenceLevel: 'Moderate',
                    identifyingFeatures: 'Narrow upright grass blades with distinct midrib and tillering cluster.',
                    uncertaintyNotes: 'DEMO SAMPLE: Young cereal grasses look similar before flowering; confirm manually.',
                    suggestedGrowthStage: 'Vegetative',
                    isDemo: true,
                  };
                  setCropRes(demo);
                  setConfirmedCrop(demo.possibleCropName);
                  setConfirmedStage(demo.suggestedGrowthStage);
                  setCropSaved(false);
                } else if (activeSubView === 'plant') {
                  const demo: PlantHealthResult = {
                    visibleSymptoms: 'Mild yellowing along older lower leaf tips with scattered tan spots.',
                    possibleExplanations: ['Early mobile nutrient stress', 'Temporary moisture imbalance', 'Early foliar spotting'],
                    suggestedChecks: ['Inspect leaf undersides for pests', 'Check ESP32 moisture & pH readings'],
                    cautiousNextSteps: ['Verify NPK via Soil Health Card before applying fertilizer', 'Consult KVK agronomist if spots spread'],
                    diagnosisLimitations: 'DEMO SAMPLE: Images alone cannot confirm pathogens or nutrient deficiencies.',
                    isDemo: true,
                  };
                  setPlantRes(demo);
                  onSaveRecord({
                    category: 'PLANT_HEALTH_SCAN',
                    origin: 'DEMO_DATA',
                    isDemo: true,
                    dataSource: 'Demo Plant Health Sample',
                    title: 'Demo Plant Health Check',
                    summary: demo.visibleSymptoms,
                    fullDetails: `${demo.visibleSymptoms}\n${demo.diagnosisLimitations}`,
                  });
                } else {
                  setLabRes({
                    nitrogen: '245',
                    phosphorus: '18.5',
                    potassium: '190',
                    unit: 'kg/ha',
                    labSource: 'Illustrative Sample Soil Health Card',
                    reportDate: '2026-10-15',
                    extractionNotes: 'DEMO SAMPLE: Verify numbers against your printed lab report before saving.',
                    isDemo: true,
                  });
                  setLabSaved(false);
                }
              }}
              className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface-variant font-label text-label-md"
            >
              Load Demo Sample
            </button>
          </div>
        </div>
      )}

      {loading && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md font-body text-body-md">
          Analyzing with Gemini via secure Vercel backend...
        </div>
      )}

      {error && (
        <div className="bg-light-error-container text-light-on-error-container rounded-lg p-gutter-md font-body text-body-sm">
          {error}
        </div>
      )}

      {/* Soil Scan Output */}
      {activeSubView === 'soil' && soilRes && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center">
            <h2 className="font-headline text-title-lg">Soil Scan Result</h2>
            <span className={`px-2.5 py-0.5 rounded-full font-label text-label-sm ${soilRes.isDemo ? 'bg-warning-container text-on-warning-container' : 'bg-success-container text-on-success-container'}`}>
              {soilRes.isDemo ? 'DEMO SAMPLE' : 'SAVED'}
            </span>
          </div>
          <p className="font-body text-body-sm"><strong>Possible Soil Type:</strong> {soilRes.possibleSoilType}</p>
          <p className="font-body text-body-sm"><strong>Visible Characteristics:</strong> {soilRes.visibleCharacteristics}</p>
          <p className="font-body text-body-sm"><strong>Organic Matter:</strong> {soilRes.organicMatterIndicators}</p>
          <p className="font-body text-body-sm"><strong>Uncertainty:</strong> {soilRes.uncertaintyExplanation}</p>
          <div>
            <strong className="font-body text-body-sm block">Additional Tests:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {soilRes.recommendedAdditionalTests.map((t, i) => <li key={i}>{t}</li>)}
            </ul>
          </div>
        </div>
      )}

      {/* Crop ID Output + User Confirmation */}
      {activeSubView === 'crop' && cropRes && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center">
            <h2 className="font-headline text-title-lg">Confirm Identified Crop</h2>
            <span className="px-2.5 py-0.5 rounded-full bg-warning-container text-on-warning-container font-label text-label-sm">
              {cropRes.isDemo ? `DEMO • ${cropRes.confidenceLevel}` : `${cropRes.confidenceLevel} Confidence`}
            </span>
          </div>
          <p className="font-body text-body-sm"><strong>Identifying Features:</strong> {cropRes.identifyingFeatures}</p>
          <p className="font-body text-body-sm"><strong>Uncertainty:</strong> {cropRes.uncertaintyNotes}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-gutter-sm">
            <label className="font-label text-label-md">
              Crop Name
              <input
                type="text"
                value={confirmedCrop}
                onChange={(e) => setConfirmedCrop(e.target.value)}
                className="mt-1 w-full rounded-sm border-light-outline-variant bg-light-surface-bright"
              />
            </label>
            <label className="font-label text-label-md">
              Growth Stage
              <select
                value={confirmedStage}
                onChange={(e) => setConfirmedStage(e.target.value)}
                className="mt-1 w-full rounded-sm border-light-outline-variant bg-light-surface-bright"
              >
                {GROWTH_STAGES.map((st) => <option key={st} value={st}>{st}</option>)}
              </select>
            </label>
          </div>
          <button
            type="button"
            disabled={cropSaved || !confirmedCrop.trim()}
            onClick={() => {
              onConfirmCrop(confirmedCrop.trim(), confirmedStage);
              onSaveRecord({
                category: 'CROP_SCAN',
                origin: cropRes.isDemo ? 'DEMO_DATA' : 'AI_INTERPRETATION',
                isDemo: Boolean(cropRes.isDemo),
                dataSource: cropRes.isDemo ? 'Demo Crop Scan' : 'Gemini Vision Crop Scan (Confirmed)',
                title: `Confirmed Crop: ${confirmedCrop.trim()} (${confirmedStage})`,
                summary: `Suggested: ${cropRes.possibleCropName} (${cropRes.confidenceLevel})`,
                fullDetails: `Confirmed Crop: ${confirmedCrop}\nStage: ${confirmedStage}\nFeatures: ${cropRes.identifyingFeatures}\nUncertainty: ${cropRes.uncertaintyNotes}`,
                imageDataUrl: previewUrl ?? undefined,
              });
              setCropSaved(true);
            }}
            className="min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
          >
            {cropSaved ? 'Saved as Active Crop' : 'Confirm & Save Active Crop'}
          </button>
        </div>
      )}

      {/* Plant Health Output */}
      {activeSubView === 'plant' && plantRes && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center">
            <h2 className="font-headline text-title-lg">Plant Health Observations</h2>
            <span className={`px-2.5 py-0.5 rounded-full font-label text-label-sm ${plantRes.isDemo ? 'bg-warning-container text-on-warning-container' : 'bg-success-container text-on-success-container'}`}>
              {plantRes.isDemo ? 'DEMO SAMPLE' : 'SAVED'}
            </span>
          </div>
          <p className="font-body text-body-sm"><strong>Visible Symptoms:</strong> {plantRes.visibleSymptoms}</p>
          <div>
            <strong className="font-body text-body-sm">Possible Explanations (Non-definitive):</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.possibleExplanations.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <div>
            <strong className="font-body text-body-sm">Suggested Field Checks:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.suggestedChecks.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <div>
            <strong className="font-body text-body-sm">Cautious Next Steps:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.cautiousNextSteps.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <p className="font-body text-body-sm text-on-warning-container bg-warning-container p-2 rounded">
            <strong>Limitations:</strong> {plantRes.diagnosisLimitations}
          </p>
        </div>
      )}

      {/* NPK Method 1: Editable Lab Report */}
      {activeSubView === 'npk' && npkMethod === 'lab' && labRes && (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <h2 className="font-headline text-title-lg">Review &amp; Correct Extracted NPK</h2>
          <p className="font-body text-body-sm text-light-on-surface-variant">{labRes.extractionNotes}</p>
          <div className="grid grid-cols-3 gap-gutter-sm">
            <label className="font-label text-label-sm">
              Nitrogen (N)
              <input value={labRes.nitrogen} onChange={(e) => setLabRes({ ...labRes, nitrogen: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              Phosphorus (P)
              <input value={labRes.phosphorus} onChange={(e) => setLabRes({ ...labRes, phosphorus: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              Potassium (K)
              <input value={labRes.potassium} onChange={(e) => setLabRes({ ...labRes, potassium: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-gutter-sm">
            <label className="font-label text-label-sm">
              Unit
              <input value={labRes.unit} onChange={(e) => setLabRes({ ...labRes, unit: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              Report Date
              <input value={labRes.reportDate} onChange={(e) => setLabRes({ ...labRes, reportDate: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              Lab Source
              <input value={labRes.labSource} onChange={(e) => setLabRes({ ...labRes, labSource: e.target.value })} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
          </div>
          <button
            type="button"
            disabled={labSaved}
            onClick={() => {
              onSaveRecord({
                category: 'NPK_REPORT',
                origin: labRes.isDemo ? 'DEMO_DATA' : 'LAB_REPORT',
                isDemo: Boolean(labRes.isDemo),
                dataSource: labRes.labSource,
                title: `${labRes.isDemo ? 'Demo ' : ''}Soil Lab NPK Report`,
                summary: `N: ${labRes.nitrogen} • P: ${labRes.phosphorus} • K: ${labRes.potassium} (${labRes.unit})`,
                fullDetails: `Lab: ${labRes.labSource}\nDate: ${labRes.reportDate}\nN: ${labRes.nitrogen} ${labRes.unit}\nP: ${labRes.phosphorus} ${labRes.unit}\nK: ${labRes.potassium} ${labRes.unit}`,
                imageDataUrl: previewUrl ?? undefined,
              });
              setLabSaved(true);
            }}
            className="min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
          >
            {labSaved ? 'Saved (Original Report Preserved)' : 'Save Verified Lab Report'}
          </button>
        </div>
      )}

      {/* NPK Method 2: Regional Reference Estimate */}
      {activeSubView === 'npk' && npkMethod === 'regional' && (
        <div className="flex flex-col gap-gutter-md">
          <div className="bg-warning-container text-on-warning-container rounded-lg p-gutter-md font-label text-label-md">
            REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST. Never treats regional extension references as measured field NPK levels.
          </div>

          <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
            <label className="font-label text-label-sm">
              Crop *
              <input value={regCrop} onChange={(e) => setRegCrop(e.target.value)} className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              State / District / Region *
              <input value={regLocation} onChange={(e) => setRegLocation(e.target.value)} placeholder="e.g., Ludhiana, Punjab" className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <label className="font-label text-label-sm">
              Soil Type (optional)
              <input value={regSoilType} onChange={(e) => setRegSoilType(e.target.value)} placeholder="e.g., Alluvial loam" className="mt-1 w-full rounded-sm border-light-outline-variant" />
            </label>
            <button
              type="button"
              disabled={loading || !regCrop.trim() || !regLocation.trim()}
              onClick={async () => {
                setError(null);
                setLoading(true);
                try {
                  const res = await soilApi.fetchRegionalNpk(regCrop, regLocation, regSoilType);
                  setRegRes(res);
                  onSaveRecord({
                    category: 'NPK_REPORT',
                    origin: 'REFERENCE_ESTIMATE',
                    isDemo: false,
                    dataSource: res.source,
                    title: `Regional Reference — ${res.crop} (${res.region})`,
                    summary: `REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST. ${res.sufficiencyStatus}`,
                    fullDetails: `REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.\nCrop: ${res.crop}\nRegion: ${res.region}\nSource: ${res.source} (${res.referenceDate})\nOverview: ${res.nutrientOverview}\nLimitations: ${res.limitations}`,
                  });
                } catch (err) {
                  setError(err instanceof Error ? err.message : 'Regional lookup failed.');
                } finally {
                  setLoading(false);
                }
              }}
              className="min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              Look Up Regional Reference
            </button>
          </div>

          {regRes && (
            <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-2">
              <span className="self-start px-3 py-1 rounded-full bg-warning-container text-on-warning-container font-label text-label-sm">
                REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.
              </span>
              {regRes.isInsufficientInfo && (
                <p className="font-label text-label-md text-on-warning-container">
                  Insufficient reliable reference data found for this specific location and crop.
                </p>
              )}
              <p className="font-body text-body-sm"><strong>Status:</strong> {regRes.sufficiencyStatus}</p>
              <p className="font-body text-body-sm"><strong>Overview:</strong> {regRes.nutrientOverview}</p>
              <p className="font-body text-body-sm"><strong>Source &amp; Date:</strong> {regRes.source} ({regRes.referenceDate})</p>
              <p className="font-body text-body-sm"><strong>Limitations:</strong> {regRes.limitations}</p>
            </div>
          )}
        </div>
      )}
    </main>
  );
};
