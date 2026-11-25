import { CROP_PRESETS, DEFAULT_CALIBRATION, type SoilRecord, type UserSettings } from '../types/soil';

const RECORDS_KEY = 'smart_soil_records_v1';
const SETTINGS_KEY = 'smart_soil_settings_v1';

export const DEFAULT_SETTINGS: UserSettings = {
  cropName: CROP_PRESETS[0].name,
  growthStage: 'Vegetative',
  minMoisture: CROP_PRESETS[0].minMoisturePct,
  maxMoisture: CROP_PRESETS[0].maxMoisturePct,
  minPh: CROP_PRESETS[0].minPh,
  maxPh: CROP_PRESETS[0].maxPh,
  calibration: DEFAULT_CALIBRATION,
};

export interface SaveRecordOutcome {
  records: SoilRecord[];
  saved: boolean;
  strippedImage: boolean;
  error?: string;
}

export const soilStorage = {
  loadRecords(): SoilRecord[] {
    try {
      const raw = localStorage.getItem(RECORDS_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? (parsed as SoilRecord[]) : [];
    } catch {
      return [];
    }
  },

  saveRecords(records: SoilRecord[]): boolean {
    try {
      localStorage.setItem(RECORDS_KEY, JSON.stringify(records));
      return true;
    } catch {
      return false;
    }
  },

  addRecordSafe(record: Omit<SoilRecord, 'id' | 'timestampMs'>): SaveRecordOutcome {
    const existing = this.loadRecords();
    const entry: SoilRecord = {
      ...record,
      id: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `rec_${Date.now()}`,
      timestampMs: Date.now(),
    };

    const next = [entry, ...existing];
    if (this.saveRecords(next)) {
      return { records: next, saved: true, strippedImage: false };
    }

    // Retry without the new photo thumbnail if localStorage quota was exceeded
    const entryNoImg: SoilRecord = { ...entry, imageDataUrl: undefined };
    const nextNoNewImg = [entryNoImg, ...existing];
    if (this.saveRecords(nextNoNewImg)) {
      return { records: nextNoNewImg, saved: true, strippedImage: Boolean(record.imageDataUrl) };
    }

    // Fallback: prune older embedded thumbnails while preserving all text reports
    const pruned = nextNoNewImg.map((item, idx) =>
      idx === 0 ? item : { ...item, imageDataUrl: undefined }
    );
    if (this.saveRecords(pruned)) {
      return { records: pruned, saved: true, strippedImage: Boolean(record.imageDataUrl) };
    }

    return {
      records: existing,
      saved: false,
      strippedImage: false,
      error: 'Browser storage is full or disabled. Delete older reports in Reports tab to save new ones.',
    };
  },

  addRecord(record: Omit<SoilRecord, 'id' | 'timestampMs'>): SoilRecord[] {
    return this.addRecordSafe(record).records;
  },

  deleteRecord(id: string): SoilRecord[] {
    const next = this.loadRecords().filter((r) => r.id !== id);
    this.saveRecords(next);
    return next;
  },

  clearDemoRecords(): SoilRecord[] {
    const next = this.loadRecords().filter((r) => !r.isDemo);
    this.saveRecords(next);
    return next;
  },

  resetAll(): void {
    try {
      localStorage.removeItem(RECORDS_KEY);
      localStorage.removeItem(SETTINGS_KEY);
    } catch {
      // Ignore storage errors on reset
    }
  },

  loadSettings(): UserSettings {
    try {
      const raw = localStorage.getItem(SETTINGS_KEY);
      if (!raw) return DEFAULT_SETTINGS;
      const parsed = JSON.parse(raw) as Partial<UserSettings>;
      return {
        ...DEFAULT_SETTINGS,
        ...parsed,
        calibration: {
          ...DEFAULT_CALIBRATION,
          ...(parsed.calibration ?? {}),
        },
      };
    } catch {
      return DEFAULT_SETTINGS;
    }
  },

  saveSettings(settings: UserSettings): boolean {
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
      return true;
    } catch {
      return false;
    }
  },
};
