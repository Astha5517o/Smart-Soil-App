import React, { useEffect, useState } from 'react';
import {
  CROP_PRESETS,
  GROWTH_STAGES,
  type BleConnectionStatus,
  type SensorReading,
  type UserSettings,
} from '../types/soil';
import { isWebBluetoothSupported } from '../services/bleService';

interface BleCalibrationModalProps {
  open: boolean;
  bleStatus: BleConnectionStatus;
  deviceName: string | null;
  lastReading: SensorReading | null;
  bleError: string | null;
  settings: UserSettings;
  onConnectBle: () => void;
  onDisconnectBle: () => void;
  onSaveSettings: (next: UserSettings) => void;
  onClose: () => void;
}

export const BleCalibrationModal: React.FC<BleCalibrationModalProps> = ({
  open,
  bleStatus,
  deviceName,
  lastReading,
  bleError,
  settings,
  onConnectBle,
  onDisconnectBle,
  onSaveSettings,
  onClose,
}) => {
  const [draft, setDraft] = useState<UserSettings>(settings);

  useEffect(() => {
    if (open) {
      setDraft(settings);
    }
  }, [open, settings]);

  if (!open) return null;

  const supported = isWebBluetoothSupported();

  return (
    <div className="fixed inset-0 z-50 bg-black/45 flex items-end sm:items-center justify-center p-4" onClick={onClose}>
      <div
        className="bg-light-surface rounded-lg max-w-lg w-full max-h-[88vh] overflow-y-auto p-gutter-lg flex flex-col gap-gutter-md"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <div>
            <span className="font-label text-label-sm uppercase tracking-wider text-light-primary">
              FarmerBuddy · SMART SOIL
            </span>
            <h2 className="font-headline text-headline-md">ESP32 BLE &amp; Calibration</h2>
          </div>
          <button type="button" onClick={onClose} className="font-label text-label-md text-light-primary">
            Close
          </button>
        </div>

        {/* Web Bluetooth Connection Card */}
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <strong className="font-headline text-title-md">Nearby BLE Device Scanner</strong>
            <span className="px-2.5 py-0.5 rounded-full bg-light-surface-container font-label text-label-sm">
              {bleStatus.toUpperCase()}
            </span>
          </div>

          {!supported && (
            <p className="bg-warning-container text-on-warning-container p-2 rounded font-body text-body-sm">
              Web Bluetooth is unavailable in this browser (use Chrome/Edge on Android, macOS, or Windows over HTTPS). Use Demo Mode for exhibition fallback.
            </p>
          )}

          <p className="font-mono text-[11px] text-light-on-surface-variant break-all">
            Service: {draft.calibration.serviceUuid}
            <br />
            Char: {draft.calibration.characteristicUuid}
          </p>

          <p className="font-body text-body-sm text-light-on-surface-variant">Tap scan to open the browser BLE device picker. Select your powered-on ESP32 (for example, SMART_SOIL) to connect.</p>\n          {deviceName && <p className="font-body text-body-sm text-light-primary">Selected device: {deviceName}</p>}
          {lastReading && (
            <p className="font-mono text-[11px] text-light-on-surface-variant">
              Last packet: {new Date(lastReading.timestampMs).toLocaleTimeString()}
            </p>
          )}
          {bleError && <p className="font-body text-body-sm text-light-error">{bleError}</p>}

          <div className="flex gap-2 mt-1">
            {bleStatus === 'connected' ? (
              <button
                type="button"
                onClick={onDisconnectBle}
                className="flex-1 min-h-[44px] rounded-full border border-light-outline-variant font-label text-label-md"
              >
                Disconnect ESP32
              </button>
            ) : (
              <button
                type="button"
                disabled={!supported || bleStatus === 'connecting'}
                onClick={onConnectBle}
                className="flex-1 min-h-[44px] rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-md disabled:opacity-50"
              >
                {bleStatus === 'connecting' ? 'Searching / connecting...' : 'Scan nearby BLE devices'}
              </button>
            )}
          </div>
        </section>

        {/* Crop & Thresholds */}
        <section className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col gap-2">
          <strong className="font-headline text-title-md">Crop &amp; Sensor Calibration</strong>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <label className="font-label text-label-sm">
              Crop Profile
              <select
                value={draft.cropName}
                onChange={(e) => {
                  const found = CROP_PRESETS.find((c) => c.name === e.target.value);
                  setDraft({
                    ...draft,
                    cropName: e.target.value,
                    minMoisture: found?.minMoisturePct ?? draft.minMoisture,
                    maxMoisture: found?.maxMoisturePct ?? draft.maxMoisture,
                    minPh: found?.minPh ?? draft.minPh,
                    maxPh: found?.maxPh ?? draft.maxPh,
                  });
                }}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              >
                {CROP_PRESETS.map((c) => (
                  <option key={c.id} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="font-label text-label-sm">
              Growth Stage
              <select
                value={draft.growthStage}
                onChange={(e) => setDraft({ ...draft, growthStage: e.target.value })}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              >
                {GROWTH_STAGES.map((st) => (
                  <option key={st} value={st}>
                    {st}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <label className="font-label text-label-sm">
              Min Moist %
              <input
                type="number"
                value={draft.minMoisture}
                onChange={(e) => setDraft({ ...draft, minMoisture: Number(e.target.value) })}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Max Moist %
              <input
                type="number"
                value={draft.maxMoisture}
                onChange={(e) => setDraft({ ...draft, maxMoisture: Number(e.target.value) })}
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Dry ADC (0%)
              <input
                type="number"
                value={draft.calibration.dryAdc}
                onChange={(e) =>
                  setDraft({ ...draft, calibration: { ...draft.calibration, dryAdc: Number(e.target.value) } })
                }
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
            <label className="font-label text-label-sm">
              Wet ADC (100%)
              <input
                type="number"
                value={draft.calibration.wetAdc}
                onChange={(e) =>
                  setDraft({ ...draft, calibration: { ...draft.calibration, wetAdc: Number(e.target.value) } })
                }
                className="mt-1 w-full rounded-sm border border-light-outline-variant bg-light-surface-container-lowest px-3 py-2 font-body text-body-md"
              />
            </label>
          </div>

          <button
            type="button"
            onClick={() => {
              onSaveSettings(draft);
              onClose();
            }}
            className="mt-2 min-h-[44px] rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-md"
          >
            Save Thresholds &amp; Calibration
          </button>
        </section>
      </div>
    </div>
  );
};
