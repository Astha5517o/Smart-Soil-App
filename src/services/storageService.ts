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

  saveRecords(records: SoilRecord[]): void {
    localStorage.setItem(RECORDS_KEY, JSON.stringify(records));
  },

  addRecord(record: Omit<SoilRecord, 'id' | 'timestampMs'>): SoilRecord[] {
    const entry: SoilRecord = {
      ...record,
      id: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `rec_${Date.now()}`,
      timestampMs: Date.now(),
    };
    const next = [entry, ...this.loadRecords()];
    this.saveRecords(next);
    return next;
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
    localStorage.removeItem(RECORDS_KEY);
    localStorage.removeItem(SETTINGS_KEY);
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

  saveSettings(settings: UserSettings): void {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  },
};
