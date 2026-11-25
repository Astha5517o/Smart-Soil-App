import React, { useEffect, useState } from 'react';
import { compressImageFile, createStorageThumbnail, soilApi } from '../services/apiService';
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
  onSaveRecord: (record: Omit<SoilRecord, 'id' | 'timestampMs'>) => boolean;
}

interface PreparedImage {
  base64: string;
  mimeType: string;
  previewUrl: string;
  fileName: string;
}

const UNREADABLE_RE = /^(not readable|unreadable|unclear|missing|n\/a|unknown)?$/i;

function isUnclearValue(val: string): boolean {
  return UNREADABLE_RE.test(val.trim());
}

export const ScanSection: React.FC<ScanSectionProps> = ({
  activeSubView,
  cropName,
  onChangeSubView,
  onConfirmCrop,
  onSaveRecord,
}) => {
  const [preparedImage, setPreparedImage] = useState<PreparedImage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [soilRes, setSoilRes] = useState<SoilScanResult | null>(null);
  const [soilSaved, setSoilSaved] = useState(false);

  const [cropRes, setCropRes] = useState<CropIdResult | null>(null);
  const [confirmedCrop, setConfirmedCrop] = useState('');
  const [confirmedStage, setConfirmedStage] = useState<string>(GROWTH_STAGES[1]);
  const [cropSaved, setCropSaved] = useState(false);

  const [plantRes, setPlantRes] = useState<PlantHealthResult | null>(null);
  const [plantSaved, setPlantSaved] = useState(false);

  const [npkMethod, setNpkMethod] = useState<'lab' | 'regional'>('lab');
  const [labRes, setLabRes] = useState<NpkLabResult | null>(null);
  const [labSaved, setLabSaved] = useState(false);

  const [regCrop, setRegCrop] = useState(cropName);
  const [regLocation, setRegLocation] = useState('');
  const [regSoilType, setRegSoilType] = useState('');
  const [regRes, setRegRes] = useState<NpkRegionalResult | null>(null);
  const [regSaved, setRegSaved] = useState(false);

  useEffect(() => {
    setError(null);
    setLoading(false);
  }, [activeSubView]);

  const clearScanOutputs = () => {
    setError(null);
    setSoilRes(null);
    setSoilSaved(false);
    setCropRes(null);
    setCropSaved(false);
    setPlantRes(null);
    setPlantSaved(false);
    setLabRes(null);
    setLabSaved(false);
  };

  const resetAllScannerState = () => {
    setPreparedImage(null);
    setLoading(false);
    clearScanOutputs();
  };

  const handleSelectFile = async (file: File) => {
    clearScanOutputs();
    try {
      const compressed = await compressImageFile(file);
      setPreparedImage({
        ...compressed,
        fileName: file.name || 'Selected photo',
      });
    } catch (err) {
      setPreparedImage(null);
      setError(err instanceof Error ? err.message : 'Could not prepare the selected image.');
    }
  };

  const handleStartScan = async (kind: 'soil' | 'crop' | 'plant' | 'npk') => {
    if (!preparedImage) {
      setError('Please take a photo or upload an image first.');
      return;
    }
    clearScanOutputs();
    setLoading(true);
    try {
      if (kind === 'soil') {
        const res = await soilApi.analyzeSoil(preparedImage.base64, preparedImage.mimeType);
        setSoilRes(res);
      } else if (kind === 'crop') {
        const res = await soilApi.identifyCrop(preparedImage.base64, preparedImage.mimeType);
        setCropRes(res);
        setConfirmedCrop(res.possibleCropName);
        setConfirmedStage(res.suggestedGrowthStage || GROWTH_STAGES[1]);
      } else if (kind === 'plant') {
        const res = await soilApi.analyzePlantHealth(preparedImage.base64, preparedImage.mimeType, cropName);
        setPlantRes(res);
      } else {
        const res = await soilApi.extractNpkReport(preparedImage.base64, preparedImage.mimeType);
        setLabRes(res);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Analysis failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleLoadDemo = () => {
    clearScanOutputs();
    setPreparedImage(null);
    if (activeSubView === 'soil') {
      setSoilRes({
        visibleCharacteristics: 'Dark brown crumbly topsoil with fine root pieces.',
        possibleSoilType: 'Alluvial Clay Loam (visual estimate)',
        organicMatterIndicators: 'Moderate humus darkening on top layer.',
        uncertaintyExplanation: 'DEMO SAMPLE: Photos cannot measure soil pH, moisture %, or NPK.',
        recommendedAdditionalTests: ['Check moisture with calibrated ESP32 probe', 'Run Soil Health Card NPK lab test'],
        isDemo: true,
      });
    } else if (activeSubView === 'crop') {
      const demo: CropIdResult = {
        possibleCropName: 'Paddy (Rice)',
        confidenceLevel: 'Moderate',
        identifyingFeatures: 'Narrow upright green blades with clear center vein.',
        uncertaintyNotes: 'DEMO SAMPLE: Young cereal grasses look similar. Please confirm.',
        suggestedGrowthStage: 'Vegetative',
        isDemo: true,
      };
      setCropRes(demo);
      setConfirmedCrop(demo.possibleCropName);
      setConfirmedStage(demo.suggestedGrowthStage);
    } else if (activeSubView === 'plant') {
      setPlantRes({
        visibleSymptoms: 'Mild yellowing along older lower leaf tips with small tan spots.',
        possibleExplanations: ['Early nutrient stress', 'Water stress or waterlogging', 'Early leaf spotting'],
        suggestedChecks: ['Check underside of leaves for insects', 'Check ESP32 moisture & pH readings'],
        cautiousNextSteps: ['Confirm nutrients with Soil Health Card before spraying', 'Ask local KVK officer if spots spread'],
        diagnosisLimitations: 'DEMO SAMPLE: A photo alone cannot confirm a plant disease.',
        isDemo: true,
      });
    } else {
      setLabRes({
        nitrogen: '245',
        phosphorus: '18.5',
        potassium: '190',
        unit: 'kg/ha',
        labSource: 'Sample Soil Health Card',
        reportDate: '2026-10-15',
        extractionNotes: 'DEMO SAMPLE: Check all numbers against your paper card before saving.',
        isUnreadable: false,
        isDemo: true,
      });
    }
  };

  if (activeSubView === 'hub') {
    return (
      <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
        <div>
          <span className="font-label text-label-sm uppercase tracking-wider text-light-primary">
            FarmerBuddy · SMART SOIL
          </span>
          <h1 className="font-headline text-headline-lg text-light-on-surface">Scan &amp; Check</h1>
          <p className="font-body text-body-md text-light-on-surface-variant">
            Choose what you want to scan · Active crop: <strong className="text-light-on-surface">{cropName}</strong>
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter-md">
          {[
            {
              key: 'soil' as const,
              title: 'Soil Scan',
              badge: 'Soil Photo',
              icon: 'terrain',
              bg: 'bg-soil-container text-on-soil-container',
              desc: 'Take a photo of soil to check texture and type.',
            },
            {
              key: 'crop' as const,
              title: 'Identify Crop',
              badge: 'Plant Photo',
              icon: 'eco',
              bg: 'bg-light-secondary-container text-light-on-secondary-container',
              desc: 'Identify your crop and set its growth stage.',
            },
            {
              key: 'plant' as const,
              title: 'Plant Health',
              badge: 'Leaf Photo',
              icon: 'healing',
              bg: 'bg-warning-container text-on-warning-container',
              desc: 'Check leaf spots or yellowing and safe next steps.',
            },
            {
              key: 'npk' as const,
              title: 'NPK Report',
              badge: 'Lab / Guide',
              icon: 'biotech',
              bg: 'bg-light-primary text-light-on-primary',
              desc: 'Scan a Soil Health Card or check regional NPK.',
            },
          ].map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => {
                resetAllScannerState();
                onChangeSubView(item.key);
              }}
              className="text-left min-h-[96px] bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex items-center gap-gutter-md"
            >
              <span className={`w-14 h-14 rounded flex items-center justify-center shrink-0 ${item.bg}`}>
                <span className="material-symbols-outlined text-[30px]" aria-hidden="true">{item.icon}</span>
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

  const headerMeta: Record<Exclude<ScanSubView, 'hub'>, { title: string; subtitle: string }> = {
    soil: {
      title: 'Soil Scan',
      subtitle: 'Take or upload a clear daylight photo of soil to check visible texture and type.',
    },
    crop: {
      title: 'Identify Crop',
      subtitle: 'Take or upload a photo of your crop leaves so you can confirm the active crop.',
    },
    plant: {
      title: 'Plant Health',
      subtitle: 'Take or upload a close-up leaf photo to check visible stress clues.',
    },
    npk: {
      title: 'NPK Report',
      subtitle: 'Read numbers from a Soil Health Card photo or view a cautious regional reference.',
    },
  };

  const activeMeta = headerMeta[activeSubView];
  const currentScanKind: 'soil' | 'crop' | 'plant' | 'npk' = activeSubView;
  const hasAnyResult = Boolean(
    (activeSubView === 'soil' && soilRes) ||
      (activeSubView === 'crop' && cropRes) ||
      (activeSubView === 'plant' && plantRes) ||
      (activeSubView === 'npk' && labRes)
  );

  return (
    <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
      {/* 1. Clear Title + Short Explanation */}
      <div className="flex items-start gap-gutter-sm">
        <button
          type="button"
          onClick={() => {
            resetAllScannerState();
            onChangeSubView('hub');
          }}
          className="min-h-[48px] min-w-[48px] rounded-full bg-light-surface-container flex items-center justify-center text-light-on-surface shrink-0"
          aria-label="Back to Scan Hub"
        >
          <span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>
        </button>
        <div>
          <h1 className="font-headline text-headline-md text-light-on-surface">{activeMeta.title}</h1>
          <p className="font-body text-body-sm text-light-on-surface-variant">{activeMeta.subtitle}</p>
        </div>
      </div>

      {activeSubView === 'npk' && (
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => {
              setError(null);
              setNpkMethod('lab');
            }}
            className={`flex-1 min-h-[48px] px-3 rounded-full font-label text-label-md ${
              npkMethod === 'lab' ? 'bg-light-primary text-light-on-primary clay-btn-primary' : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            1. Scan Lab Card
          </button>
          <button
            type="button"
            onClick={() => {
              setError(null);
              setNpkMethod('regional');
            }}
            className={`flex-1 min-h-[48px] px-3 rounded-full font-label text-label-md ${
              npkMethod === 'regional' ? 'bg-light-primary text-light-on-primary clay-btn-primary' : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            2. Regional Guide
          </button>
        </div>
      )}

      {/* 2. Photo Capture / Upload / Preview / Start Scan Card */}
      {(activeSubView !== 'npk' || npkMethod === 'lab') && (
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          {/* Separate Take Photo (Camera) and Upload Image (Gallery) buttons */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-gutter-sm">
            <label className="min-h-[52px] px-4 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg flex items-center justify-center gap-2 cursor-pointer hover:opacity-95">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">photo_camera</span>
              Take Photo
              <input
                type="file"
                accept="image/*"
                capture="environment"
                className="sr-only"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) void handleSelectFile(f);
                  e.target.value = '';
                }}
              />
            </label>

            <label className="min-h-[52px] px-4 rounded-full border border-light-outline-variant bg-light-surface-container-low text-light-on-surface font-label text-label-lg flex items-center justify-center gap-2 cursor-pointer hover:bg-light-surface-container">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">photo_library</span>
              Upload Image
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,image/*"
                className="sr-only"
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) void handleSelectFile(f);
                  e.target.value = '';
                }}
              />
            </label>
          </div>

          {/* Image Preview */}
          {preparedImage && (
            <div className="flex flex-col gap-2 pt-2">
              <div className="relative rounded overflow-hidden border border-light-outline-variant/60 bg-light-surface-container-low">
                <img
                  src={preparedImage.previewUrl}
                  alt="Preview before scanning"
                  className="w-full max-h-64 object-cover"
                />
              </div>

              {/* Explicit Start Scan Button */}
              {!loading && (
                <button
                  type="button"
                  onClick={() => void handleStartScan(currentScanKind)}
                  className="w-full min-h-[52px] px-6 rounded-full bg-success text-on-success font-headline text-title-md flex items-center justify-center gap-2 shadow-sm hover:opacity-95"
                >
                  <span className="material-symbols-outlined text-[22px]" aria-hidden="true">search</span>
                  {hasAnyResult ? 'Scan Photo Again' : 'Start Scan'}
                </button>
              )}
            </div>
          )}

          {/* Demo Sample Option */}
          {!loading && (
            <div className="flex justify-end">
              <button
                type="button"
                onClick={handleLoadDemo}
                className="min-h-[40px] px-3 rounded-full text-light-primary font-label text-label-sm hover:bg-light-surface-container"
              >
                Load Demo Sample
              </button>
            </div>
          )}
        </section>
      )}

      {/* 3. Visible Loading State */}
      {loading && (
        <div
          role="status"
          aria-live="polite"
          className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-lg flex items-center gap-3"
        >
          <span className="w-6 h-6 rounded-full border-4 border-light-primary border-t-transparent animate-spin shrink-0" />
          <div>
            <p className="font-headline text-title-md text-light-on-surface">Analysing...</p>
            <p className="font-body text-body-sm text-light-on-surface-variant">
              Reading your photo safely with FarmerBuddy AI.
            </p>
          </div>
        </div>
      )}

      {/* 4. Error State with Try Again */}
      {error && (
        <div role="alert" className="bg-light-error-container text-light-on-error-container rounded-lg p-gutter-md flex flex-col gap-3">
          <div className="flex items-start gap-2">
            <span className="material-symbols-outlined text-[22px] shrink-0" aria-hidden="true">error</span>
            <div>
              <strong className="block font-headline text-title-md">Scan could not finish</strong>
              <p className="font-body text-body-sm">{error}</p>
            </div>
          </div>
          {preparedImage && (activeSubView !== 'npk' || npkMethod === 'lab') && (
            <button
              type="button"
              onClick={() => void handleStartScan(currentScanKind)}
              className="self-start min-h-[44px] px-5 rounded-full bg-light-error text-light-on-error font-label text-label-md"
            >
              Try Again
            </button>
          )}
        </div>
      )}

      {/* 5A. Soil Scan Complete Card */}
      {activeSubView === 'soil' && soilRes && (
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center gap-2">
            <div>
              <span className="font-label text-label-sm text-success uppercase">Scan Complete</span>
              <h2 className="font-headline text-title-lg text-light-on-surface">{soilRes.possibleSoilType}</h2>
            </div>
            <span
              className={`px-3 py-1 rounded-full font-label text-label-sm ${
                soilRes.isDemo
                  ? 'bg-warning-container text-on-warning-container'
                  : 'bg-success-container text-on-success-container'
              }`}
            >
              {soilRes.isDemo ? 'DEMO SAMPLE' : soilSaved ? 'SAVED' : 'READY TO SAVE'}
            </span>
          </div>

          <p className="font-body text-body-sm"><strong>Visible Look:</strong> {soilRes.visibleCharacteristics}</p>
          <p className="font-body text-body-sm"><strong>Organic Matter:</strong> {soilRes.organicMatterIndicators}</p>
          <p className="font-body text-body-sm bg-warning-container/70 text-on-warning-container p-2.5 rounded">
            <strong>Note:</strong> {soilRes.uncertaintyExplanation}
          </p>
          <div>
            <strong className="font-body text-body-sm block">Helpful Next Tests:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {soilRes.recommendedAdditionalTests.map((t, i) => <li key={i}>{t}</li>)}
            </ul>
          </div>

          <div className="flex flex-wrap gap-2 pt-2">
            <button
              type="button"
              disabled={soilSaved}
              onClick={async () => {
                const thumb = preparedImage ? await createStorageThumbnail(preparedImage.previewUrl) : undefined;
                const savedOk = onSaveRecord({
                  category: 'SOIL_SCAN',
                  origin: soilRes.isDemo ? 'DEMO_DATA' : 'AI_INTERPRETATION',
                  isDemo: Boolean(soilRes.isDemo),
                  dataSource: soilRes.isDemo ? 'Demo Soil Scan Sample' : 'FarmerBuddy Vision Soil Scan',
                  title: `${soilRes.isDemo ? 'Demo ' : ''}Soil Scan — ${soilRes.possibleSoilType}`,
                  summary: soilRes.visibleCharacteristics,
                  fullDetails: `Visible: ${soilRes.visibleCharacteristics}\nPossible type: ${soilRes.possibleSoilType}\nOrganic matter: ${soilRes.organicMatterIndicators}\nNote: ${soilRes.uncertaintyExplanation}\nTests: ${soilRes.recommendedAdditionalTests.join('; ')}`,
                  imageDataUrl: thumb,
                });
                if (savedOk) setSoilSaved(true);
              }}
              className="flex-1 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              {soilSaved ? 'Saved to Reports' : 'Save Result'}
            </button>
            <button
              type="button"
              onClick={resetAllScannerState}
              className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface font-label text-label-md"
            >
              Scan Again
            </button>
          </div>
        </section>
      )}

      {/* 5B. Crop ID Complete Card */}
      {activeSubView === 'crop' && cropRes && (
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center gap-2">
            <div>
              <span className="font-label text-label-sm text-success uppercase">Scan Complete</span>
              <h2 className="font-headline text-title-lg text-light-on-surface">Confirm Your Crop</h2>
            </div>
            <span className="px-3 py-1 rounded-full bg-warning-container text-on-warning-container font-label text-label-sm">
              {cropRes.isDemo ? `DEMO · ${cropRes.confidenceLevel}` : `${cropRes.confidenceLevel} Confidence`}
            </span>
          </div>

          <p className="font-body text-body-sm"><strong>Visible Features:</strong> {cropRes.identifyingFeatures}</p>
          <p className="font-body text-body-sm text-light-on-surface-variant"><strong>Check:</strong> {cropRes.uncertaintyNotes}</p>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-gutter-sm">
            <label className="font-label text-label-md">
              Crop Name
              <input
                type="text"
                value={confirmedCrop}
                onChange={(e) => {
                  setConfirmedCrop(e.target.value);
                  setCropSaved(false);
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2.5 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-md">
              Growth Stage
              <select
                value={confirmedStage}
                onChange={(e) => {
                  setConfirmedStage(e.target.value);
                  setCropSaved(false);
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2.5 font-body text-body-md"
              >
                {GROWTH_STAGES.map((st) => <option key={st} value={st}>{st}</option>)}
              </select>
            </label>
          </div>

          <div className="flex flex-wrap gap-2 pt-2">
            <button
              type="button"
              disabled={cropSaved || !confirmedCrop.trim()}
              onClick={async () => {
                const thumb = preparedImage ? await createStorageThumbnail(preparedImage.previewUrl) : undefined;
                onConfirmCrop(confirmedCrop.trim(), confirmedStage);
                const savedOk = onSaveRecord({
                  category: 'CROP_SCAN',
                  origin: cropRes.isDemo ? 'DEMO_DATA' : 'AI_INTERPRETATION',
                  isDemo: Boolean(cropRes.isDemo),
                  dataSource: cropRes.isDemo ? 'Demo Crop Scan' : 'FarmerBuddy Vision Crop Scan (Confirmed)',
                  title: `Confirmed Crop: ${confirmedCrop.trim()} (${confirmedStage})`,
                  summary: `Suggested: ${cropRes.possibleCropName} (${cropRes.confidenceLevel})`,
                  fullDetails: `Confirmed Crop: ${confirmedCrop}\nStage: ${confirmedStage}\nFeatures: ${cropRes.identifyingFeatures}\nNotes: ${cropRes.uncertaintyNotes}`,
                  imageDataUrl: thumb,
                });
                if (savedOk) setCropSaved(true);
              }}
              className="flex-1 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              {cropSaved ? 'Saved Active Crop' : 'Save Result'}
            </button>
            <button
              type="button"
              onClick={resetAllScannerState}
              className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface font-label text-label-md"
            >
              Scan Again
            </button>
          </div>
        </section>
      )}

      {/* 5C. Plant Health Complete Card */}
      {activeSubView === 'plant' && plantRes && (
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center gap-2">
            <div>
              <span className="font-label text-label-sm text-success uppercase">Scan Complete</span>
              <h2 className="font-headline text-title-lg text-light-on-surface">Leaf &amp; Plant Check</h2>
            </div>
            <span
              className={`px-3 py-1 rounded-full font-label text-label-sm ${
                plantRes.isDemo
                  ? 'bg-warning-container text-on-warning-container'
                  : 'bg-success-container text-on-success-container'
              }`}
            >
              {plantRes.isDemo ? 'DEMO SAMPLE' : plantSaved ? 'SAVED' : 'READY TO SAVE'}
            </span>
          </div>

          <p className="font-body text-body-sm"><strong>Visible Signs:</strong> {plantRes.visibleSymptoms}</p>
          <div>
            <strong className="font-body text-body-sm">Possible Reasons (Not a final diagnosis):</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.possibleExplanations.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <div>
            <strong className="font-body text-body-sm">What to Check in Field:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.suggestedChecks.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <div>
            <strong className="font-body text-body-sm">Safe Next Steps:</strong>
            <ul className="list-disc pl-5 font-body text-body-sm text-light-on-surface-variant">
              {plantRes.cautiousNextSteps.map((x, i) => <li key={i}>{x}</li>)}
            </ul>
          </div>
          <p className="font-body text-body-sm text-on-warning-container bg-warning-container p-2.5 rounded">
            <strong>Important:</strong> {plantRes.diagnosisLimitations}
          </p>

          <div className="flex flex-wrap gap-2 pt-2">
            <button
              type="button"
              disabled={plantSaved}
              onClick={async () => {
                const thumb = preparedImage ? await createStorageThumbnail(preparedImage.previewUrl) : undefined;
                const savedOk = onSaveRecord({
                  category: 'PLANT_HEALTH_SCAN',
                  origin: plantRes.isDemo ? 'DEMO_DATA' : 'AI_INTERPRETATION',
                  isDemo: Boolean(plantRes.isDemo),
                  dataSource: plantRes.isDemo ? 'Demo Plant Health Sample' : 'FarmerBuddy Vision Plant Check',
                  title: `${plantRes.isDemo ? 'Demo ' : ''}Plant Health — ${cropName}`,
                  summary: plantRes.visibleSymptoms,
                  fullDetails: `Symptoms: ${plantRes.visibleSymptoms}\nPossible reasons: ${plantRes.possibleExplanations.join('; ')}\nChecks: ${plantRes.suggestedChecks.join('; ')}\nNext steps: ${plantRes.cautiousNextSteps.join('; ')}\nLimitations: ${plantRes.diagnosisLimitations}`,
                  imageDataUrl: thumb,
                });
                if (savedOk) setPlantSaved(true);
              }}
              className="flex-1 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              {plantSaved ? 'Saved to Reports' : 'Save Result'}
            </button>
            <button
              type="button"
              onClick={resetAllScannerState}
              className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface font-label text-label-md"
            >
              Scan Again
            </button>
          </div>
        </section>
      )}

      {/* 5D. NPK Method 1: Editable Lab Report */}
      {activeSubView === 'npk' && npkMethod === 'lab' && labRes && (() => {
        const stillUnreadable =
          Boolean(labRes.isUnreadable) &&
          isUnclearValue(labRes.nitrogen) &&
          isUnclearValue(labRes.phosphorus) &&
          isUnclearValue(labRes.potassium);
        return (
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
          <div className="flex justify-between items-center gap-2">
            <div>
              <span className="font-label text-label-sm text-success uppercase">Scan Complete</span>
              <h2 className="font-headline text-title-lg text-light-on-surface">Check Extracted NPK</h2>
            </div>
            <span
              className={`px-3 py-1 rounded-full font-label text-label-sm ${
                labRes.isDemo
                  ? 'bg-warning-container text-on-warning-container'
                  : stillUnreadable
                  ? 'bg-light-error-container text-light-on-error-container'
                  : 'bg-success-container text-on-success-container'
              }`}
            >
              {labRes.isDemo ? 'DEMO SAMPLE' : stillUnreadable ? 'UNCLEAR PHOTO' : labSaved ? 'SAVED' : 'EDIT & VERIFY'}
            </span>
          </div>

          {stillUnreadable && (
            <div className="bg-light-error-container text-light-on-error-container rounded p-2.5 font-body text-body-sm">
              N, P, K numbers could not be clearly read from this photo. Take a closer, sharper photo or type the values from your card below.
            </div>
          )}

          <p className="font-body text-body-sm text-light-on-surface-variant">{labRes.extractionNotes}</p>

          <div className="grid grid-cols-3 gap-gutter-sm">
            {[
              { label: 'Nitrogen (N)', value: labRes.nitrogen, key: 'nitrogen' as const },
              { label: 'Phosphorus (P)', value: labRes.phosphorus, key: 'phosphorus' as const },
              { label: 'Potassium (K)', value: labRes.potassium, key: 'potassium' as const },
            ].map((field) => {
              const unclear = isUnclearValue(field.value);
              return (
                <label key={field.key} className="font-label text-label-sm">
                  {field.label}
                  <input
                    value={field.value}
                    onChange={(e) => {
                      setLabSaved(false);
                      setLabRes({ ...labRes, [field.key]: e.target.value });
                    }}
                    className={`mt-1 w-full rounded-sm px-3 py-2 font-body text-body-md ${
                      unclear
                        ? 'border-2 border-light-error bg-light-error-container/30 text-light-on-error-container'
                        : 'border border-light-outline-variant bg-light-surface-container-lowest'
                    }`}
                  />
                </label>
              );
            })}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-gutter-sm">
            <label className="font-label text-label-sm">
              Unit
              <input
                value={labRes.unit}
                onChange={(e) => {
                  setLabSaved(false);
                  setLabRes({ ...labRes, unit: e.target.value });
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Report Date
              <input
                value={labRes.reportDate}
                onChange={(e) => {
                  setLabSaved(false);
                  setLabRes({ ...labRes, reportDate: e.target.value });
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Lab Source
              <input
                value={labRes.labSource}
                onChange={(e) => {
                  setLabSaved(false);
                  setLabRes({ ...labRes, labSource: e.target.value });
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
          </div>

          <div className="flex flex-wrap gap-2 pt-2">
            <button
              type="button"
              disabled={labSaved}
              onClick={async () => {
                const thumb = preparedImage ? await createStorageThumbnail(preparedImage.previewUrl) : undefined;
                const savedOk = onSaveRecord({
                  category: 'NPK_REPORT',
                  origin: labRes.isDemo ? 'DEMO_DATA' : 'LAB_REPORT',
                  isDemo: Boolean(labRes.isDemo),
                  dataSource: labRes.labSource || 'Uploaded Soil Health Card',
                  title: `${labRes.isDemo ? 'Demo ' : ''}Soil Lab NPK Report`,
                  summary: `N: ${labRes.nitrogen} • P: ${labRes.phosphorus} • K: ${labRes.potassium} (${labRes.unit})`,
                  fullDetails: `Lab: ${labRes.labSource}\nDate: ${labRes.reportDate}\nN: ${labRes.nitrogen} ${labRes.unit}\nP: ${labRes.phosphorus} ${labRes.unit}\nK: ${labRes.potassium} ${labRes.unit}`,
                  imageDataUrl: thumb,
                });
                if (savedOk) setLabSaved(true);
              }}
              className="flex-1 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              {labSaved ? 'Saved (Original Card Kept)' : 'Save Result'}
            </button>
            <button
              type="button"
              onClick={resetAllScannerState}
              className="min-h-[48px] px-4 rounded-full bg-light-surface-container text-light-on-surface font-label text-label-md"
            >
              Scan Again
            </button>
          </div>
        </section>
        );
      })()}

      {/* 5E. NPK Method 2: Regional Reference Estimate */}
      {activeSubView === 'npk' && npkMethod === 'regional' && (
        <div className="flex flex-col gap-gutter-md">
          <div className="bg-warning-container text-on-warning-container rounded-lg p-gutter-md font-label text-label-md">
            REFERENCE ESTIMATE — NOT A LAB SOIL TEST. Shows general area advice only, never measured field NPK numbers.
          </div>

          <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-gutter-sm">
            <label className="font-label text-label-sm">
              Crop *
              <input
                value={regCrop}
                onChange={(e) => setRegCrop(e.target.value)}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2.5 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              State / District / Region *
              <input
                value={regLocation}
                onChange={(e) => setRegLocation(e.target.value)}
                placeholder="e.g., Ludhiana, Punjab"
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2.5 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Soil Type (optional)
              <input
                value={regSoilType}
                onChange={(e) => setRegSoilType(e.target.value)}
                placeholder="e.g., Alluvial loam"
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2.5 font-body text-body-md"
              />
            </label>
            <button
              type="button"
              disabled={loading || !regCrop.trim() || !regLocation.trim()}
              onClick={async () => {
                setError(null);
                setRegRes(null);
                setRegSaved(false);
                setLoading(true);
                try {
                  const res = await soilApi.fetchRegionalNpk(regCrop, regLocation, regSoilType);
                  setRegRes(res);
                } catch (err) {
                  setError(err instanceof Error ? err.message : 'Regional lookup failed.');
                } finally {
                  setLoading(false);
                }
              }}
              className="min-h-[50px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
            >
              {loading ? 'Analysing...' : 'Get Regional Guide'}
            </button>
          </div>

          {regRes && (
            <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-2">
              <span className="self-start px-3 py-1 rounded-full bg-warning-container text-on-warning-container font-label text-label-sm">
                REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST
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

              <button
                type="button"
                disabled={regSaved}
                onClick={() => {
                  const savedOk = onSaveRecord({
                    category: 'NPK_REPORT',
                    origin: 'REFERENCE_ESTIMATE',
                    isDemo: false,
                    dataSource: regRes.source,
                    title: `Regional Reference — ${regRes.crop} (${regRes.region})`,
                    summary: `REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST. ${regRes.sufficiencyStatus}`,
                    fullDetails: `REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.\nCrop: ${regRes.crop}\nRegion: ${regRes.region}\nSource: ${regRes.source} (${regRes.referenceDate})\nOverview: ${regRes.nutrientOverview}\nLimitations: ${regRes.limitations}`,
                  });
                  if (savedOk) setRegSaved(true);
                }}
                className="mt-2 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg disabled:opacity-60"
              >
                {regSaved ? 'Saved to Reports' : 'Save Result'}
              </button>
            </div>
          )}
        </div>
      )}
    </main>
  );
};
