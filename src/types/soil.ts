export interface CropProfile {
  id: string;
  name: string;
  minMoisturePct: number;
  maxMoisturePct: number;
  minPh: number;
  maxPh: number;
  optimalTempMinC: number;
  optimalTempMaxC: number;
}

export const CROP_PRESETS: CropProfile[] = [
  { id: 'paddy', name: 'Paddy (Rice)', minMoisturePct: 38, maxMoisturePct: 55, minPh: 5.5, maxPh: 7.0, optimalTempMinC: 20, optimalTempMaxC: 32 },
  { id: 'wheat', name: 'Wheat', minMoisturePct: 28, maxMoisturePct: 45, minPh: 6.0, maxPh: 7.5, optimalTempMinC: 15, optimalTempMaxC: 25 },
  { id: 'maize', name: 'Maize (Corn)', minMoisturePct: 30, maxMoisturePct: 50, minPh: 5.8, maxPh: 7.0, optimalTempMinC: 18, optimalTempMaxC: 30 },
  { id: 'tomato', name: 'Tomato', minMoisturePct: 35, maxMoisturePct: 55, minPh: 6.0, maxPh: 6.8, optimalTempMinC: 18, optimalTempMaxC: 29 },
  { id: 'cotton', name: 'Cotton', minMoisturePct: 25, maxMoisturePct: 45, minPh: 6.0, maxPh: 8.0, optimalTempMinC: 20, optimalTempMaxC: 32 },
  { id: 'pulses', name: 'Pulses / Chickpea', minMoisturePct: 22, maxMoisturePct: 40, minPh: 6.0, maxPh: 7.5, optimalTempMinC: 16, optimalTempMaxC: 28 },
];

export const GROWTH_STAGES = ['Seedling', 'Vegetative', 'Flowering / Reproductive', 'Maturity'] as const;

export type SensorDisplayMode = 'live' | 'demo_normal' | 'demo_alert';

export type BleConnectionStatus =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'unsupported'
  | 'error';

export interface CalibrationConfig {
  dryAdc: number;
  wetAdc: number;
  serviceUuid: string;
  characteristicUuid: string;
}

export const DEFAULT_CALIBRATION: CalibrationConfig = {
  dryAdc: 3200,
  wetAdc: 1400,
  serviceUuid: '4fafc201-1fb5-459e-8fcc-c5c9c331914b',
  characteristicUuid: 'beb5483e-36e1-4688-b7f5-ea07361b26a8',
};

export interface SensorReading {
  moistureRaw: number | null;
  moisturePercent: number | null;
  ph: number | null;
  temperatureC: number | null;
  timestampMs: number;
  isDemo: boolean;
  validationWarnings: string[];
}

export interface SoilScanResult {
  visibleCharacteristics: string;
  possibleSoilType: string;
  organicMatterIndicators: string;
  uncertaintyExplanation: string;
  recommendedAdditionalTests: string[];
  isDemo?: boolean;
}

export interface CropIdResult {
  possibleCropName: string;
  confidenceLevel: string;
  identifyingFeatures: string;
  uncertaintyNotes: string;
  suggestedGrowthStage: string;
  isDemo?: boolean;
}

export interface PlantHealthResult {
  visibleSymptoms: string;
  possibleExplanations: string[];
  suggestedChecks: string[];
  cautiousNextSteps: string[];
  diagnosisLimitations: string;
  isDemo?: boolean;
}

export interface NpkLabResult {
  nitrogen: string;
  phosphorus: string;
  potassium: string;
  unit: string;
  labSource: string;
  reportDate: string;
  extractionNotes: string;
  isDemo?: boolean;
}

export interface NpkRegionalResult {
  crop: string;
  region: string;
  soilType: string;
  sufficiencyStatus: string;
  nutrientOverview: string;
  source: string;
  referenceDate: string;
  limitations: string;
  isInsufficientInfo: boolean;
  isDemo?: boolean;
}

export interface AiRecommendationItem {
  category: string;
  identifiedIssue: string;
  whyItMatters: string;
  suggestedNextStep: string;
  supportingEvidence: string;
  limitationsAndUncertainty: string;
}

export interface AiAdvisoryPlan {
  summary: string;
  recommendations: AiRecommendationItem[];
  contextSnapshot: string;
  isDemo?: boolean;
}

export type RecordCategory =
  | 'SENSOR_READING'
  | 'SOIL_SCAN'
  | 'CROP_SCAN'
  | 'PLANT_HEALTH_SCAN'
  | 'NPK_REPORT'
  | 'AI_RECOMMENDATION';

export type RecordOrigin =
  | 'LIVE_SENSOR'
  | 'DEMO_DATA'
  | 'LAB_REPORT'
  | 'REFERENCE_ESTIMATE'
  | 'AI_INTERPRETATION';

export interface SoilRecord {
  id: string;
  timestampMs: number;
  category: RecordCategory;
  origin: RecordOrigin;
  isDemo: boolean;
  dataSource: string;
  title: string;
  summary: string;
  fullDetails: string;
  imageDataUrl?: string;
}

export interface UserSettings {
  cropName: string;
  growthStage: string;
  minMoisture: number;
  maxMoisture: number;
  minPh: number;
  maxPh: number;
  calibration: CalibrationConfig;
}
