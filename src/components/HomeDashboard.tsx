import React from 'react';
import type { BleConnectionStatus, SensorDisplayMode, SensorReading, UserSettings } from '../types/soil';

interface HomeDashboardProps {
  settings: UserSettings;
  bleStatus: BleConnectionStatus;
  bleError: string | null;
  displayMode: SensorDisplayMode;
  reading: SensorReading | null;
  onSelectMode: (mode: SensorDisplayMode) => void;
  onOpenBleModal: () => void;
  onStartScan: () => void;
  onQuickScanSoil: () => void;
  onQuickIdentifyCrop: () => void;
  onQuickPlantHealth: () => void;
  onQuickNpkReport: () => void;
  onOpenAdvisor: () => void;
  onSaveSnapshot: () => void;
}

export const HomeDashboard: React.FC<HomeDashboardProps> = ({
  settings,
  bleStatus,
  bleError,
  displayMode,
  reading,
  onSelectMode,
  onOpenBleModal,
  onStartScan,
  onQuickScanSoil,
  onQuickIdentifyCrop,
  onQuickPlantHealth,
  onQuickNpkReport,
  onOpenAdvisor,
  onSaveSnapshot,
}) => {
  const isLiveMode = displayMode === 'live';
  const isConnected = bleStatus === 'connected';
  const hasLiveReading = isConnected && reading !== null && !reading.isDemo;

  const mVal = reading?.moisturePercent ?? null;
  const dryCutoff = Math.max(5, settings.minMoisture - 6);
  const moistureStatus: {
    label: string;
    hindi: string;
    badgeClass: string;
    barClass: string;
    cardTint: string;
    hint: string;
  } = (() => {
    if (mVal === null) {
      return {
        label: isConnected ? 'Waiting for data' : 'Not connected',
        hindi: isConnected ? 'डेटा की प्रतीक्षा' : 'सेंसर जुड़ा नहीं है',
        badgeClass: 'bg-light-surface-container text-light-on-surface-variant',
        barClass: 'bg-light-outline',
        cardTint: 'border border-light-outline-variant/60',
        hint: isConnected ? 'Bluetooth is connected. Waiting for a valid sensor reading from the ESP32.' : 'Connect your ESP32 sensor or tap Demo to preview.',
      };
    }
    if (mVal < dryCutoff) {
      return {
        label: 'Dry',
        hindi: 'सूखी मिट्टी',
        badgeClass: 'bg-light-error-container text-light-on-error-container',
        barClass: 'bg-light-error',
        cardTint: 'border-2 border-light-error/35',
        hint: 'Dry — check plot before watering.',
      };
    }
    if (mVal < settings.minMoisture) {
      return {
        label: 'Moderate',
        hindi: 'हल्की नमी',
        badgeClass: 'bg-warning-container text-on-warning-container',
        barClass: 'bg-warning',
        cardTint: 'border-2 border-warning/35',
        hint: 'Getting dry — watch moisture closely.',
      };
    }
    if (mVal > settings.maxMoisture) {
      return {
        label: 'Moderate',
        hindi: 'अधिक नमी',
        badgeClass: 'bg-warning-container text-on-warning-container',
        barClass: 'bg-warning',
        cardTint: 'border-2 border-warning/35',
        hint: 'Very wet — pause watering and check drainage.',
      };
    }
    return {
      label: 'Moist',
      hindi: 'अच्छी नमी',
      badgeClass: 'bg-success-container text-on-success-container',
      barClass: 'bg-success',
      cardTint: 'border-2 border-success/30',
      hint: `Good moisture for ${settings.cropName}.`,
    };
  })();

  const phVal = reading?.ph ?? null;
  const phLow = phVal !== null && phVal < settings.minPh;
  const phHigh = phVal !== null && phVal > settings.maxPh;
  const phStatusLabel =
    phVal === null
      ? isLiveMode && !isConnected
        ? 'Not connected'
        : isConnected ? 'Waiting for data' : 'Unavailable'
      : phLow
      ? 'Acidic'
      : phHigh
      ? 'Alkaline'
      : 'Near Neutral';
  const phWarn = phLow || phHigh;

  const tVal = reading?.temperatureC ?? null;
  const tWarm = tVal !== null && tVal > 30;
  const tCool = tVal !== null && tVal < 15;
  const tStatusLabel =
    tVal === null
      ? isLiveMode && !isConnected
        ? 'Not connected'
        : isConnected ? 'Waiting for data' : 'Unavailable'
      : tWarm
      ? 'Warm'
      : tCool
      ? 'Cool'
      : 'Optimal';

  const lastUpdatedLabel = reading
    ? new Date(reading.timestampMs).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    : 'Not connected';

  const connectionLabel =
    isLiveMode && isConnected
      ? 'ESP32 Live'
      : isLiveMode
      ? 'Not connected'
      : 'Demo Preview';

  return (
    <div className="flex flex-col flex-1">
      {/* FarmerBuddy / SMART SOIL Header */}
      <header className="sticky top-0 z-20 bg-light-surface/95 backdrop-blur border-b border-light-outline-variant/60 px-page-margin py-gutter-sm">
        <div className="flex items-center justify-between gap-gutter-sm">
          <button
            type="button"
            onClick={onOpenBleModal}
            className="flex items-center gap-gutter-sm text-left hover:opacity-90 transition-opacity"
          >
            <div className="w-11 h-11 rounded bg-light-primary text-light-on-primary clay-btn-primary flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">eco</span>
            </div>
            <div>
              <h1 className="font-headline text-title-lg text-light-on-surface leading-tight">
                FarmerBuddy
              </h1>
              <p className="font-label text-label-sm tracking-wider text-light-primary uppercase">
                SMART SOIL · {settings.cropName}
              </p>
            </div>
          </button>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={onOpenBleModal}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full min-h-[36px] ${
                isConnected
                  ? 'bg-success-container text-on-success-container'
                  : bleStatus === 'unsupported'
                  ? 'bg-light-error-container text-light-on-error-container'
                  : 'bg-light-surface-container text-light-on-surface-variant'
              }`}
            >
              <span
                className={`w-2.5 h-2.5 rounded-full ${
                  isConnected ? 'bg-success' : bleStatus === 'unsupported' ? 'bg-light-error' : 'bg-light-outline'
                }`}
                aria-hidden="true"
              />
              <span className="font-label text-label-sm">
                {hasLiveReading ? 'ESP32 Live' : isConnected ? 'BLE Connected' : bleStatus === 'unsupported' ? 'No BLE' : 'ESP32 Off'}
              </span>
            </button>

            <span
              role="status"
              className={`inline-flex items-center px-2.5 py-1.5 rounded-full font-label text-label-sm ${
                isLiveMode && isConnected
                  ? 'bg-success-container text-on-success-container'
                  : 'bg-warning-container text-on-warning-container'
              }`}
            >
              {isLiveMode && hasLiveReading ? 'LIVE DATA' : isLiveMode && isConnected ? 'WAITING' : isLiveMode ? 'OFFLINE' : 'DEMO'}
            </span>
          </div>
        </div>

        {/* Simple Mode Switch + Calibration Button */}
        <div className="mt-gutter-sm flex items-center gap-1.5" role="group" aria-label="Sensor display mode">
          <button
            type="button"
            onClick={() => {
              onSelectMode('live');
              if (!isConnected) onOpenBleModal();
            }}
            className={`flex-1 min-h-[40px] py-1.5 px-2 rounded-full font-label text-label-sm transition-colors ${
              displayMode === 'live'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            Live ESP32
          </button>
          <button
            type="button"
            onClick={() => onSelectMode('demo_normal')}
            className={`flex-1 min-h-[40px] py-1.5 px-2 rounded-full font-label text-label-sm transition-colors ${
              displayMode === 'demo_normal'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            Demo
          </button>
          <button
            type="button"
            onClick={() => onSelectMode('demo_alert')}
            className={`flex-1 min-h-[40px] py-1.5 px-2 rounded-full font-label text-label-sm transition-colors ${
              displayMode === 'demo_alert'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            Dry Alert
          </button>
          <button
            type="button"
            onClick={onOpenBleModal}
            aria-label="Sensor and crop settings"
            className="w-10 h-10 rounded-full bg-light-surface-container text-light-on-surface flex items-center justify-center shrink-0"
          >
            <span className="material-symbols-outlined text-[20px]" aria-hidden="true">tune</span>
          </button>
        </div>
      </header>

      <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
        {/* Honest Disconnected Banner in Live Mode */}
        {isLiveMode && (!isConnected || !hasLiveReading) && (
          <div className="bg-warning-container text-on-warning-container rounded-lg p-gutter-md flex flex-wrap items-center justify-between gap-gutter-sm">
            <div>
              <strong className="block font-headline text-title-md">{isConnected ? 'Waiting for Sensor Data' : 'Sensor Not Connected'}</strong>
              <p className="font-body text-body-sm">
                {bleError || '{bleError || (isConnected ? 'Bluetooth link is active, but no valid sensor packet has arrived yet.' : 'Connect your ESP32 via Bluetooth or switch to Demo.')}'}
              </p>
            </div>
            <button
              type="button"
              onClick={onOpenBleModal}
              className="min-h-[44px] px-4 rounded-full bg-light-primary text-light-on-primary font-label text-label-md inline-flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-[18px]" aria-hidden="true">bluetooth</span>
              Connect
            </button>
          </div>
        )}

        {/* LARGE SOIL MOISTURE CARD (Understandable in 3 seconds) */}
        <section
          aria-label="Soil Moisture Status"
          className={`bg-light-surface-container-lowest rounded-lg clay-card p-gutter-lg flex flex-col gap-3 ${moistureStatus.cardTint}`}
        >
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center gap-2.5">
              <div className="w-12 h-12 rounded bg-light-secondary-container text-light-on-secondary-container flex items-center justify-center shrink-0">
                <span className="material-symbols-outlined text-[28px]" aria-hidden="true">water_drop</span>
              </div>
              <div>
                <h2 className="font-headline text-title-lg text-light-on-surface">Soil Moisture</h2>
                <p className="font-body text-body-sm text-light-on-surface-variant">
                  {moistureStatus.hindi} · Target {settings.minMoisture}–{settings.maxMoisture}%
                </p>
              </div>
            </div>

            <span className={`px-3.5 py-1.5 rounded-full font-label text-label-md shrink-0 ${moistureStatus.badgeClass}`}>
              {moistureStatus.label}
            </span>
          </div>

          <div className="flex items-baseline justify-between gap-2 flex-wrap">
            <span className="font-mono text-[44px] leading-none font-bold text-light-on-surface tabular-nums">
              {mVal !== null ? `${mVal}%` : isConnected ? 'Waiting for data' : 'Not connected'}
            </span>
            {reading && (
              <button
                type="button"
                onClick={onSaveSnapshot}
                className="min-h-[40px] px-3.5 rounded-full border border-light-outline-variant bg-light-surface-container-low text-light-on-surface font-label text-label-sm inline-flex items-center gap-1.5 hover:bg-light-surface-container"
              >
                <span className="material-symbols-outlined text-[17px]" aria-hidden="true">save</span>
                Save Reading
              </button>
            )}
          </div>

          {/* Red / Yellow / Green Visual Progress Scale */}
          <div className="w-full h-4 rounded-full bg-light-surface-container clay-inset overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${moistureStatus.barClass}`}
              style={{ width: `${mVal ?? 0}%` }}
            />
          </div>

          <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-light-outline-variant/40 text-body-sm">
            <span className="font-body text-light-on-surface">{moistureStatus.hint}</span>
            <span className="font-mono text-[11px] text-light-on-surface-variant">
              {connectionLabel} · Updated {lastUpdatedLabel}
            </span>
          </div>
        </section>

        {/* SMALLER pH & TEMPERATURE CARDS */}
        <section aria-label="Soil pH and Temperature" className="grid grid-cols-2 gap-gutter-sm">
          {/* Soil pH */}
          <article className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col justify-between gap-2">
            <div className="flex items-center justify-between gap-1">
              <span className="font-headline text-title-md text-light-on-surface flex items-center gap-1.5">
                <span className="material-symbols-outlined text-soil text-[20px]" aria-hidden="true">science</span>
                Soil pH
              </span>
              <span
                className={`px-2 py-0.5 rounded-full font-label text-label-sm ${
                  phVal === null
                    ? 'bg-light-surface-container text-light-on-surface-variant'
                    : phWarn
                    ? 'bg-warning-container text-on-warning-container'
                    : 'bg-success-container text-on-success-container'
                }`}
              >
                {phStatusLabel}
              </span>
            </div>

            <div className="font-mono text-[26px] leading-tight font-bold text-light-on-surface tabular-nums">
              {phVal !== null ? phVal.toFixed(1) : isLiveMode && !isConnected ? 'Not connected' : 'Unavailable'}
            </div>

            <p className="font-body text-body-sm text-light-on-surface-variant">
              Ideal {settings.minPh}–{settings.maxPh}
            </p>
          </article>

          {/* Soil Temperature */}
          <article className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col justify-between gap-2">
            <div className="flex items-center justify-between gap-1">
              <span className="font-headline text-title-md text-light-on-surface flex items-center gap-1.5">
                <span className="material-symbols-outlined text-warning text-[20px]" aria-hidden="true">thermostat</span>
                Temp
              </span>
              <span
                className={`px-2 py-0.5 rounded-full font-label text-label-sm ${
                  tVal === null
                    ? 'bg-light-surface-container text-light-on-surface-variant'
                    : tWarm || tCool
                    ? 'bg-warning-container text-on-warning-container'
                    : 'bg-success-container text-on-success-container'
                }`}
              >
                {tStatusLabel}
              </span>
            </div>

            <div className="font-mono text-[26px] leading-tight font-bold text-light-on-surface tabular-nums">
              {tVal !== null ? `${tVal.toFixed(1)}°C` : isLiveMode && !isConnected ? 'Not connected' : 'Unavailable'}
            </div>

            <p className="font-body text-body-sm text-light-on-surface-variant">
              Root zone · तापमान
            </p>
          </article>
        </section>

        {/* PROMINENT PRIMARY CTA: SCAN YOUR SOIL */}
        <button
          type="button"
          onClick={onStartScan}
          className="w-full min-h-[56px] px-6 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-headline text-title-lg flex items-center justify-center gap-3 hover:opacity-95 transition-opacity"
        >
          <span className="material-symbols-outlined text-[26px]" aria-hidden="true">photo_camera</span>
          SCAN YOUR SOIL
        </button>

        {/* FOUR SIMPLE FEATURE CARDS */}
        <section aria-label="Scanner tools" className="grid grid-cols-2 md:grid-cols-4 gap-gutter-sm">
          <button
            type="button"
            onClick={onQuickScanSoil}
            className="text-left min-h-[112px] bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col justify-between gap-2"
          >
            <span className="w-11 h-11 rounded bg-soil-container text-on-soil-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">terrain</span>
            </span>
            <div>
              <span className="block font-headline text-title-md text-light-on-surface">Soil Scan</span>
              <span className="block font-body text-body-sm text-light-on-surface-variant">Check soil type</span>
            </div>
          </button>

          <button
            type="button"
            onClick={onQuickIdentifyCrop}
            className="text-left min-h-[112px] bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col justify-between gap-2"
          >
            <span className="w-11 h-11 rounded bg-light-secondary-container text-light-on-secondary-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">eco</span>
            </span>
            <div>
              <span className="block font-headline text-title-md text-light-on-surface">Identify Crop</span>
              <span className="block font-body text-body-sm text-light-on-surface-variant">Confirm plant</span>
            </div>
          </button>

          <button
            type="button"
            onClick={onQuickPlantHealth}
            className="text-left min-h-[112px] bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col justify-between gap-2"
          >
            <span className="w-11 h-11 rounded bg-warning-container text-on-warning-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">healing</span>
            </span>
            <div>
              <span className="block font-headline text-title-md text-light-on-surface">Plant Health</span>
              <span className="block font-body text-body-sm text-light-on-surface-variant">Leaf check</span>
            </div>
          </button>

          <button
            type="button"
            onClick={onQuickNpkReport}
            className="text-left min-h-[112px] bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col justify-between gap-2"
          >
            <span className="w-11 h-11 rounded bg-light-primary text-light-on-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[24px]" aria-hidden="true">biotech</span>
            </span>
            <div>
              <span className="block font-headline text-title-md text-light-on-surface">NPK Report</span>
              <span className="block font-body text-body-sm text-light-on-surface-variant">Lab card / guide</span>
            </div>
          </button>
        </section>

        {/* Compact AI Advisor Link */}
        <section
          aria-label="FarmerBuddy AI Advisor"
          className="bg-light-primary-container text-light-on-primary-container rounded-lg clay-btn-primary p-gutter-md flex flex-wrap items-center justify-between gap-gutter-sm"
        >
          <div className="flex items-center gap-gutter-sm">
            <span className="material-symbols-outlined text-[26px] shrink-0" aria-hidden="true">psychology</span>
            <div>
              <span className="font-label text-label-sm uppercase tracking-wide opacity-85 block">Ask FarmerBuddy</span>
              <p className="font-body text-body-sm">Get simple next steps for your crop &amp; soil</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenAdvisor}
            className="min-h-[44px] px-4 rounded-full bg-light-surface-container-lowest text-light-primary font-label text-label-md inline-flex items-center gap-1"
          >
            AI Advisor
            <span className="material-symbols-outlined text-[16px]" aria-hidden="true">arrow_forward</span>
          </button>
        </section>
      </main>
    </div>
  );
};
