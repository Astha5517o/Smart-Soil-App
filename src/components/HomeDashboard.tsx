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
  onOpenAdvisor,
  onSaveSnapshot,
}) => {
  const isLiveMode = displayMode === 'live';
  const isConnected = bleStatus === 'connected';

  const mVal = reading?.moisturePercent ?? null;
  const mLow = mVal !== null && mVal < settings.minMoisture;
  const mHigh = mVal !== null && mVal > settings.maxMoisture;
  const mPill = mVal === null ? 'Unavailable' : mLow ? 'Low' : mHigh ? 'High' : 'Suitable';
  const mWarn = mVal === null || mLow || mHigh;

  const phVal = reading?.ph ?? null;
  const phLow = phVal !== null && phVal < settings.minPh;
  const phHigh = phVal !== null && phVal > settings.maxPh;
  const phPill = phVal === null ? 'Unavailable' : phLow ? 'Acidic' : phHigh ? 'Alkaline' : 'Near Neutral';
  const phWarn = phVal === null || phLow || phHigh;
  const phMarkerPct = phVal !== null ? Math.max(4, Math.min(94, ((phVal - 3.5) / 7.0) * 100)) : 50;

  const tVal = reading?.temperatureC ?? null;
  const tWarm = tVal !== null && tVal > 30;
  const tPill = tVal === null ? 'Unavailable' : tWarm ? 'Warm' : 'Optimal';

  const adviceText = (() => {
    if (!reading) {
      return 'Connect your ESP32 sensor node via Web Bluetooth for live moisture, pH, and temperature guidance—or switch to Demo Mode for exhibition preview.';
    }
    const prefix = reading.isDemo ? '[DEMO] ' : '';
    if (mLow && phHigh) {
      return `${prefix}Soil moisture (${mVal}%) is below your ${settings.cropName} threshold and pH (${phVal}) is above its preferred range. Check another area of the plot and review a reliable soil test before choosing an amendment.`;
    }
    if (mLow) {
      return `${prefix}Soil moisture (${mVal}%) is below the ${settings.minMoisture}% target for ${settings.cropName}. Check another spot in the plot before scheduling irrigation.`;
    }
    if (phLow || phHigh) {
      return `${prefix}Soil pH (${phVal}) is outside ${settings.cropName}'s preferred ${settings.minPh}–${settings.maxPh} range. Never amend from a single reading alone—confirm with a laboratory soil test.`;
    }
    return `${prefix}Moisture (${mVal ?? '--'}%) and pH (${phVal ?? '--'}) are within your ${settings.cropName} crop's preferred range. Keep monitoring morning probe readings before scheduling irrigation.`;
  })();

  return (
    <div className="flex flex-col flex-1">
      {/* Top App Header matching Stitch HomeDashboard.html */}
      <header className="sticky top-0 z-20 bg-light-surface border-b border-light-outline-variant px-page-margin py-gutter-sm">
        <div className="flex flex-wrap items-center justify-between gap-gutter-sm">
          <button
            type="button"
            onClick={onOpenBleModal}
            className="flex items-center gap-gutter-sm text-left hover:opacity-90 transition-opacity"
          >
            <div className="w-10 h-10 rounded bg-light-primary text-light-on-primary clay-btn-primary flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">compost</span>
            </div>
            <div>
              <h1 className="font-headline text-title-lg text-light-on-surface leading-tight">SMART SOIL</h1>
              <p className="font-body text-body-sm text-light-on-surface-variant">
                Crop: <strong className="text-light-on-surface">{settings.cropName}</strong>
              </p>
            </div>
          </button>

          <div className="flex items-center gap-gutter-xs flex-wrap justify-end">
            <button
              type="button"
              onClick={onOpenBleModal}
              className={`inline-flex items-center gap-gutter-xs px-gutter-sm py-gutter-xs rounded-full ${
                isConnected
                  ? 'bg-success-container text-on-success-container'
                  : bleStatus === 'unsupported'
                  ? 'bg-light-error-container text-light-on-error-container'
                  : 'bg-light-surface-container text-light-on-surface-variant'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  isConnected ? 'bg-success' : bleStatus === 'unsupported' ? 'bg-light-error' : 'bg-light-outline'
                }`}
                aria-hidden="true"
              />
              <span className="font-label text-label-sm">
                {isConnected ? 'ESP32 Connected' : bleStatus === 'unsupported' ? 'BLE Unsupported' : 'ESP32 Offline'}
              </span>
            </button>

            <span
              role="status"
              className={`inline-flex items-center gap-gutter-xs px-gutter-sm py-gutter-xs rounded-full ${
                isLiveMode && isConnected
                  ? 'bg-success-container text-on-success-container'
                  : 'bg-warning-container text-on-warning-container'
              }`}
            >
              <span className="font-label text-label-sm">{isLiveMode ? 'LIVE DATA' : 'DEMO DATA'}</span>
            </span>
          </div>
        </div>

        {/* Exhibition Demo & BLE Switcher */}
        <div className="mt-gutter-sm flex items-center gap-gutter-xs overflow-x-auto pb-gutter-xs" role="group" aria-label="Exhibition display mode">
          <button
            type="button"
            onClick={() => {
              onSelectMode('live');
              if (!isConnected) onOpenBleModal();
            }}
            className={`px-gutter-sm py-1 rounded-full font-label text-label-sm shrink-0 ${
              displayMode === 'live'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            {isConnected ? 'Live ESP32' : 'Live ESP32 (Connect)'}
          </button>

          <button
            type="button"
            onClick={() => onSelectMode('demo_normal')}
            className={`px-gutter-sm py-1 rounded-full font-label text-label-sm shrink-0 ${
              displayMode === 'demo_normal'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            Demo Mode (Normal)
          </button>

          <button
            type="button"
            onClick={() => onSelectMode('demo_alert')}
            className={`px-gutter-sm py-1 rounded-full font-label text-label-sm shrink-0 ${
              displayMode === 'demo_alert'
                ? 'bg-light-primary text-light-on-primary clay-btn-primary'
                : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            Demo Mode (Low Moisture &amp; High pH Alert)
          </button>

          <button
            type="button"
            onClick={onOpenBleModal}
            className="px-gutter-sm py-1 rounded-full bg-light-surface-container text-light-on-surface-variant font-label text-label-sm shrink-0 inline-flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[15px]" aria-hidden="true">tune</span>
            BLE &amp; Thresholds
          </button>
        </div>
      </header>

      <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
        {/* Main Heading + START SCAN CTA */}
        <div className="flex flex-wrap items-center justify-between gap-gutter-sm">
          <div className="flex flex-wrap items-baseline gap-gutter-sm">
            <h2 className="font-headline text-headline-md text-light-on-surface">Your Soil Health at a Glance</h2>
            {!isLiveMode && (
              <span className="px-gutter-sm py-0.5 rounded-full bg-warning-container text-on-warning-container font-label text-label-sm">
                Illustrative Example Readings (Demo)
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={onStartScan}
            className="inline-flex items-center gap-2 min-h-[48px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-lg hover:opacity-95 transition-opacity"
          >
            <span className="material-symbols-outlined text-[20px]" aria-hidden="true">photo_camera</span>
            START SCAN
          </button>
        </div>

        {/* Honest Disconnected Warning in Live Mode */}
        {isLiveMode && !isConnected && (
          <div className="bg-warning-container text-on-warning-container rounded-lg p-gutter-md flex flex-wrap items-center justify-between gap-gutter-sm">
            <div>
              <strong className="block font-headline text-title-md">ESP32 Not Connected — Live Readings Unavailable</strong>
              <p className="font-body text-body-sm mt-0.5">
                {bleError || 'Connect your ESP32 DevKit V1 over Web Bluetooth to stream live moisture, pH, and temperature packets, or switch to Demo Mode.'}
              </p>
            </div>
            <button
              type="button"
              onClick={onOpenBleModal}
              className="min-h-[44px] px-4 rounded-full bg-light-primary text-light-on-primary font-label text-label-md inline-flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-[18px]" aria-hidden="true">bluetooth</span>
              Connect ESP32
            </button>
          </div>
        )}

        {/* Three Prominent Sensor Cards */}
        <section aria-label="ESP32 sensor readings" className="grid grid-cols-1 md:grid-cols-3 gap-gutter-md items-stretch">
          {/* 1. Moisture Card */}
          <article className="h-full bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col justify-between gap-gutter-sm">
            <div className="flex items-center justify-between">
              <span className="font-headline text-title-lg text-light-on-surface flex items-center gap-gutter-xs">
                <span className="material-symbols-outlined text-light-primary text-[22px]" aria-hidden="true">water_drop</span>
                Moisture
              </span>
              <span
                className={`px-gutter-sm py-gutter-xs rounded-full font-label text-label-sm ${
                  mWarn ? 'bg-warning-container text-on-warning-container' : 'bg-success-container text-on-success-container'
                }`}
              >
                {mPill}
              </span>
            </div>

            <div>
              <div className="flex items-baseline gap-2">
                <span className="font-mono text-display-lg text-light-on-surface tabular-nums">
                  {mVal !== null ? `${mVal}%` : '--%'}
                </span>
                <span className="font-body text-body-sm text-light-on-surface-variant">
                  Target: {settings.minMoisture}–{settings.maxMoisture}%
                </span>
              </div>
              {reading?.moistureRaw !== null && reading?.moistureRaw !== undefined && (
                <span className="font-mono text-[11px] text-light-on-surface-variant block">
                  HW-390 Raw ADC: {reading.moistureRaw}
                </span>
              )}
            </div>

            <div className="w-full h-3.5 rounded-full bg-light-surface-container clay-inset overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${mLow ? 'bg-warning' : 'bg-light-primary'}`}
                style={{ width: `${mVal ?? 0}%` }}
              />
            </div>

            <div
              className={`p-gutter-sm rounded font-body text-body-sm ${
                mWarn && mVal !== null
                  ? 'bg-warning-container text-on-warning-container'
                  : 'bg-light-surface-container-low text-light-on-surface-variant'
              }`}
            >
              <strong className="block text-light-on-surface">
                {mVal === null
                  ? 'Sensor unavailable'
                  : mLow
                  ? `Alert: Below ${settings.minMoisture}% threshold`
                  : mHigh
                  ? `Alert: Above ${settings.maxMoisture}% threshold`
                  : 'Within crop range'}
              </strong>
              <span>
                {mVal === null
                  ? 'Connect ESP32 or check HW-390 wiring.'
                  : mLow
                  ? 'Next: Check another plot spot & plan irrigation.'
                  : mHigh
                  ? 'Next: Pause irrigation & check field drainage.'
                  : 'Next: Check again tomorrow morning.'}
              </span>
            </div>
          </article>

          {/* 2. Soil pH Card */}
          <article className="h-full bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col justify-between gap-gutter-sm">
            <div className="flex items-center justify-between">
              <span className="font-headline text-title-lg text-light-on-surface flex items-center gap-gutter-xs">
                <span className="material-symbols-outlined text-soil text-[22px]" aria-hidden="true">science</span>
                Soil pH
              </span>
              <span
                className={`px-gutter-sm py-gutter-xs rounded-full font-label text-label-sm ${
                  phWarn ? 'bg-warning-container text-on-warning-container' : 'bg-success-container text-on-success-container'
                }`}
              >
                {phPill}
              </span>
            </div>

            <div className="flex items-baseline gap-2">
              <span className="font-mono text-display-lg text-light-on-surface tabular-nums">
                {phVal !== null ? phVal.toFixed(1) : '--'}
              </span>
              <span className="font-body text-body-sm text-light-on-surface-variant">
                Preferred: {settings.minPh}–{settings.maxPh}
              </span>
            </div>

            <div className="flex flex-col gap-1">
              <div className="relative w-full h-3.5 rounded-full overflow-hidden flex">
                <div className="flex-1 bg-warning/70" />
                <div className="flex-1 bg-success/75" />
                <div className="flex-1 bg-soil/70" />
                {phVal !== null && (
                  <div
                    className="absolute top-0 bottom-0 w-2 bg-light-on-surface rounded-full border border-light-surface-container-lowest transition-all duration-500"
                    style={{ left: `${phMarkerPct}%` }}
                  />
                )}
              </div>
              <div className="flex justify-between font-mono text-[11px] text-light-on-surface-variant tabular-nums">
                <span>Acidic</span>
                <span>Neutral (7.0)</span>
                <span>Alkaline</span>
              </div>
            </div>

            <div
              className={`p-gutter-sm rounded font-body text-body-sm ${
                phWarn && phVal !== null
                  ? 'bg-warning-container text-on-warning-container'
                  : 'bg-light-surface-container-low text-light-on-surface-variant'
              }`}
            >
              <strong className="block text-light-on-surface">
                {phVal === null
                  ? 'pH probe unavailable'
                  : phLow || phHigh
                  ? `Alert: Outside ${settings.minPh}–${settings.maxPh} range`
                  : `Balanced for ${settings.cropName.toLowerCase()}`}
              </strong>
              <span>
                {phVal === null
                  ? 'Verify ADIY BNC calibration & 3.3V divider first.'
                  : phLow || phHigh
                  ? 'Next: Review a soil test before choosing an amendment.'
                  : 'Next: No pH amendment needed today.'}
              </span>
            </div>
          </article>

          {/* 3. Temperature Card */}
          <article className="h-full bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md flex flex-col justify-between gap-gutter-sm">
            <div className="flex items-center justify-between">
              <span className="font-headline text-title-lg text-light-on-surface flex items-center gap-gutter-xs">
                <span className="material-symbols-outlined text-warning text-[22px]" aria-hidden="true">thermostat</span>
                Temperature
              </span>
              <span
                className={`px-gutter-sm py-gutter-xs rounded-full font-label text-label-sm ${
                  tVal === null
                    ? 'bg-light-surface-container text-light-on-surface-variant'
                    : tWarm
                    ? 'bg-warning-container text-on-warning-container'
                    : 'bg-success-container text-on-success-container'
                }`}
              >
                {tPill}
              </span>
            </div>

            <div className="flex items-baseline gap-2">
              <span className="font-mono text-display-lg text-light-on-surface tabular-nums">
                {tVal !== null ? `${tVal.toFixed(1)}°C` : '--°C'}
              </span>
              <span className="font-body text-body-sm text-light-on-surface-variant">DS18B20 Probe</span>
            </div>

            <div className="w-full h-3.5 rounded-full bg-light-surface-container clay-inset overflow-hidden">
              <div
                className="h-full rounded-full bg-soil transition-all duration-500"
                style={{ width: `${tVal !== null ? Math.min(100, Math.max(8, (tVal / 45) * 100)) : 0}%` }}
              />
            </div>

            <div className="p-gutter-sm rounded bg-light-surface-container-low text-light-on-surface-variant font-body text-body-sm">
              <strong className="text-light-on-surface block">Root-zone warmth</strong>
              <span>
                {tVal === null
                  ? 'Connect DS18B20 with 4.7kΩ pull-up resistor on ESP32.'
                  : tWarm
                  ? 'Higher surface warmth; organic mulch can help shade roots.'
                  : 'Supports steady root activity without midday heat stress.'}
              </span>
            </div>
          </article>
        </section>

        {reading && (
          <div className="flex justify-end">
            <button
              type="button"
              onClick={onSaveSnapshot}
              className="min-h-[44px] px-4 rounded-full border border-light-outline-variant bg-light-surface-container-lowest text-light-on-surface font-label text-label-md inline-flex items-center gap-2 hover:bg-light-surface-container-low"
            >
              <span className="material-symbols-outlined text-[18px]" aria-hidden="true">save</span>
              {reading.isDemo ? 'Save Demo Snapshot to Reports' : 'Save Live Reading to Reports'}
            </button>
          </div>
        )}

        {/* Prominent "Today's Soil Advice" Section */}
        <section
          aria-label="Today's Soil Advice"
          className="bg-light-primary-container text-light-on-primary-container rounded-lg clay-btn-primary p-gutter-md flex flex-col md:flex-row md:items-center justify-between gap-gutter-md"
        >
          <div className="flex items-start gap-gutter-sm">
            <span className="material-symbols-outlined text-[26px] shrink-0 mt-0.5" aria-hidden="true">lightbulb</span>
            <div>
              <span className="font-label text-label-sm uppercase tracking-wide opacity-90 block">
                Today&apos;s Soil Advice
              </span>
              <p className="font-body text-body-md font-medium mt-0.5">{adviceText}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenAdvisor}
            className="shrink-0 inline-flex items-center justify-center gap-gutter-xs min-h-[44px] px-gutter-md rounded-full bg-light-surface-container-lowest text-light-primary font-label text-label-lg"
          >
            Open AI Advisor
            <span className="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_forward</span>
          </button>
        </section>

        {/* Four Quick-Action Cards */}
        <section aria-label="Quick actions" className="grid grid-cols-2 md:grid-cols-4 gap-gutter-sm">
          <button
            type="button"
            onClick={onQuickScanSoil}
            className="text-left bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col items-start gap-gutter-xs"
          >
            <span className="w-10 h-10 rounded bg-soil-container text-on-soil-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">terrain</span>
            </span>
            <span className="font-headline text-title-lg text-light-on-surface">Scan Soil</span>
            <span className="font-body text-body-sm text-light-on-surface-variant">Visual soil check</span>
          </button>

          <button
            type="button"
            onClick={onQuickIdentifyCrop}
            className="text-left bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col items-start gap-gutter-xs"
          >
            <span className="w-10 h-10 rounded bg-light-secondary-container text-light-on-secondary-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">eco</span>
            </span>
            <span className="font-headline text-title-lg text-light-on-surface">Identify Crop</span>
            <span className="font-body text-body-sm text-light-on-surface-variant">Confirm crop &amp; stage</span>
          </button>

          <button
            type="button"
            onClick={onQuickPlantHealth}
            className="text-left bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col items-start gap-gutter-xs"
          >
            <span className="w-10 h-10 rounded bg-warning-container text-on-warning-container flex items-center justify-center">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">healing</span>
            </span>
            <span className="font-headline text-title-lg text-light-on-surface">Check Plant Health</span>
            <span className="font-body text-body-sm text-light-on-surface-variant">Leaf symptom scan</span>
          </button>

          <button
            type="button"
            onClick={onOpenAdvisor}
            className="text-left bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md hover:ring-2 hover:ring-light-primary transition-all flex flex-col items-start gap-gutter-xs"
          >
            <span className="w-10 h-10 rounded bg-light-primary text-light-on-primary flex items-center justify-center">
              <span className="material-symbols-outlined text-[22px]" aria-hidden="true">psychology</span>
            </span>
            <span className="font-headline text-title-lg text-light-on-surface">Get AI Advice</span>
            <span className="font-body text-body-sm text-light-on-surface-variant">Guided soil plan</span>
          </button>
        </section>
      </main>
    </div>
  );
};
