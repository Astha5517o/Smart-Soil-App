import React, { useMemo, useRef, useState } from 'react';
import { AdvisorSection } from './components/AdvisorSection';
import { BleCalibrationModal } from './components/BleCalibrationModal';
import { HomeDashboard } from './components/HomeDashboard';
import { ReportsSection } from './components/ReportsSection';
import { ScanSection, type ScanSubView } from './components/ScanSection';
import { isWebBluetoothSupported, parseAndValidateSensorJson } from './services/bleService';
import { DEFAULT_SETTINGS, soilStorage } from './services/storageService';
import type {
  BleConnectionStatus,
  SensorDisplayMode,
  SensorReading,
  SoilRecord,
  UserSettings,
} from './types/soil';

type MainTab = 'home' | 'scan' | 'advisor' | 'reports';

export const App: React.FC = () => {
  const [tab, setTab] = useState<MainTab>('home');
  const [scanSubView, setScanSubView] = useState<ScanSubView>('hub');
  const [settings, setSettings] = useState<UserSettings>(() => soilStorage.loadSettings());
  const [records, setRecords] = useState<SoilRecord[]>(() => soilStorage.loadRecords());
  const [displayMode, setDisplayMode] = useState<SensorDisplayMode>('demo_normal');

  const [bleStatus, setBleStatus] = useState<BleConnectionStatus>(() =>
    isWebBluetoothSupported() ? 'disconnected' : 'unsupported'
  );
  const [bleDeviceName, setBleDeviceName] = useState<string | null>(null);
  const [bleError, setBleError] = useState<string | null>(null);
  const [liveReading, setLiveReading] = useState<SensorReading | null>(null);
  const [showBleModal, setShowBleModal] = useState(false);

  const gattServerRef = useRef<BluetoothRemoteGATTServer | null>(null);

  const activeReading: SensorReading | null = useMemo(() => {
    if (displayMode === 'live') {
      return bleStatus === 'connected' ? liveReading : null;
    }
    if (displayMode === 'demo_alert') {
      return {
        moistureRaw: 2768,
        moisturePercent: 24,
        ph: 7.9,
        temperatureC: 31.4,
        timestampMs: Date.now(),
        isDemo: true,
        validationWarnings: [],
      };
    }
    return {
      moistureRaw: 2408,
      moisturePercent: 44,
      ph: 6.4,
      temperatureC: 26.8,
      timestampMs: Date.now(),
      isDemo: true,
      validationWarnings: [],
    };
  }, [displayMode, bleStatus, liveReading]);

  const contextSnapshot = useMemo(() => {
    const latestSoil = records.find((r) => r.category === 'SOIL_SCAN');
    const latestNpk = records.find((r) => r.category === 'NPK_REPORT');
    return [
      `Crop: ${settings.cropName} (Growth Stage: ${settings.growthStage})`,
      `Targets: Moisture ${settings.minMoisture}-${settings.maxMoisture}%, pH ${settings.minPh}-${settings.maxPh}`,
      activeReading
        ? `Sensor Source: ${activeReading.isDemo ? 'DEMO DATA (Illustrative)' : 'LIVE ESP32 BLE'}\nMoisture: ${activeReading.moisturePercent ?? 'Unavailable'}% (Raw ADC: ${activeReading.moistureRaw ?? 'N/A'})\npH: ${activeReading.ph ?? 'Unavailable'}\nTemp: ${activeReading.temperatureC ?? 'Unavailable'}°C`
        : 'Sensor Readings: ESP32 Disconnected / Unavailable',
      latestSoil ? `Latest Soil Scan: ${latestSoil.title} — ${latestSoil.summary}` : 'Latest Soil Scan: None',
      latestNpk ? `Latest NPK Info (${latestNpk.origin}): ${latestNpk.title} — ${latestNpk.summary}` : 'Latest NPK Info: None',
    ].join('\n');
  }, [settings, activeReading, records]);

  const connectBle = async () => {
    if (!isWebBluetoothSupported() || !navigator.bluetooth) {
      setBleStatus('unsupported');
      setBleError('Web Bluetooth is not supported in this browser.');
      return;
    }
    setBleError(null);
    setBleStatus('connecting');
    try {
      const device = await navigator.bluetooth.requestDevice({
        filters: [{ services: [settings.calibration.serviceUuid] }],
        optionalServices: [settings.calibration.serviceUuid],
      });
      setBleDeviceName(device.name || 'SMART_SOIL_ESP32');
      device.addEventListener('gattserverdisconnected', () => {
        setBleStatus('disconnected');
        setBleError('ESP32 disconnected. Reconnect when ready.');
      });
      const server = await device.gatt?.connect();
      if (!server) throw new Error('GATT server unavailable.');
      gattServerRef.current = server;

      const service = await server.getPrimaryService(settings.calibration.serviceUuid);
      const char = await service.getCharacteristic(settings.calibration.characteristicUuid);

      const handlePacket = (view: DataView | undefined) => {
        if (!view) return;
        const text = new TextDecoder().decode(view);
        const parsed = parseAndValidateSensorJson(text, settings.calibration);
        if (parsed.ok) {
          setLiveReading(parsed.reading);
          setBleError(parsed.reading.validationWarnings.join(' ') || null);
        } else {
          setBleError(parsed.error);
        }
      };

      char.addEventListener('characteristicvaluechanged', (ev: Event) => {
        const target = ev.target as BluetoothRemoteGATTCharacteristic | null;
        handlePacket(target?.value);
      });
      await char.startNotifications();
      const initial = await char.readValue().catch(() => undefined);
      handlePacket(initial);

      setBleStatus('connected');
      setDisplayMode('live');
    } catch (err) {
      setBleStatus('error');
      setBleError(err instanceof Error ? err.message : 'Bluetooth connection failed.');
    }
  };

  const disconnectBle = () => {
    gattServerRef.current?.disconnect();
    gattServerRef.current = null;
    setBleStatus(isWebBluetoothSupported() ? 'disconnected' : 'unsupported');
    setLiveReading(null);
  };

  const handleSaveRecord = (rec: Omit<SoilRecord, 'id' | 'timestampMs'>) => {
    setRecords(soilStorage.addRecord(rec));
  };

  return (
    <div className="min-h-screen flex flex-col max-w-5xl mx-auto bg-light-surface">
      {tab === 'home' && (
        <HomeDashboard
          settings={settings}
          bleStatus={bleStatus}
          bleError={bleError}
          displayMode={displayMode}
          reading={activeReading}
          onSelectMode={setDisplayMode}
          onOpenBleModal={() => setShowBleModal(true)}
          onStartScan={() => {
            setScanSubView('hub');
            setTab('scan');
          }}
          onQuickScanSoil={() => {
            setScanSubView('soil');
            setTab('scan');
          }}
          onQuickIdentifyCrop={() => {
            setScanSubView('crop');
            setTab('scan');
          }}
          onQuickPlantHealth={() => {
            setScanSubView('plant');
            setTab('scan');
          }}
          onOpenAdvisor={() => setTab('advisor')}
          onSaveSnapshot={() => {
            if (!activeReading) return;
            handleSaveRecord({
              category: 'SENSOR_READING',
              origin: activeReading.isDemo ? 'DEMO_DATA' : 'LIVE_SENSOR',
              isDemo: activeReading.isDemo,
              dataSource: activeReading.isDemo ? 'Demo Mode Snapshot' : `ESP32 BLE (${bleDeviceName || 'Node'})`,
              title: `${activeReading.isDemo ? 'Demo' : 'Live'} Sensor Snapshot — ${settings.cropName}`,
              summary: `Moisture: ${activeReading.moisturePercent ?? '--'}% • pH: ${activeReading.ph ?? '--'} • Temp: ${activeReading.temperatureC ?? '--'}°C`,
              fullDetails: `Source: ${activeReading.isDemo ? 'DEMO DATA' : 'LIVE ESP32 BLE'}\nMoisture: ${activeReading.moisturePercent ?? 'Unavailable'}% (Raw ADC: ${activeReading.moistureRaw ?? 'N/A'})\npH: ${activeReading.ph ?? 'Unavailable'}\nTemp: ${activeReading.temperatureC ?? 'Unavailable'}°C`,
            });
          }}
        />
      )}

      {tab === 'scan' && (
        <ScanSection
          activeSubView={scanSubView}
          cropName={settings.cropName}
          onChangeSubView={setScanSubView}
          onConfirmCrop={(cropName, growthStage) => {
            const next = { ...settings, cropName, growthStage };
            setSettings(next);
            soilStorage.saveSettings(next);
          }}
          onSaveRecord={handleSaveRecord}
        />
      )}

      {tab === 'advisor' && (
        <AdvisorSection
          contextSnapshot={contextSnapshot}
          cropName={settings.cropName}
          onSaveRecord={handleSaveRecord}
        />
      )}

      {tab === 'reports' && (
        <ReportsSection
          records={records}
          onDelete={(id) => setRecords(soilStorage.deleteRecord(id))}
          onClearDemo={() => setRecords(soilStorage.clearDemoRecords())}
          onSeedDemo={() => {
            soilStorage.addRecord({
              category: 'SENSOR_READING',
              origin: 'DEMO_DATA',
              isDemo: true,
              dataSource: 'Demo Mode Exhibition Packet',
              title: 'Demo Sensor Snapshot — Paddy Plot',
              summary: 'Moisture: 44% • pH: 6.4 • Temp: 26.8°C',
              fullDetails: 'DEMO DATA (Illustrative Example)\nMoisture Raw ADC: 2408 (44%)\npH: 6.4\nTemp: 26.8°C',
            });
            setRecords(
              soilStorage.addRecord({
                category: 'NPK_REPORT',
                origin: 'REFERENCE_ESTIMATE',
                isDemo: true,
                dataSource: 'Sample Regional Reference (Demo)',
                title: 'Paddy Regional Reference (Illustrative)',
                summary: 'REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.',
                fullDetails: 'REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.\nRegional averages vary by plot history. Use a Soil Health Card lab test before applying fertilizers.',
              })
            );
          }}
          onResetAll={() => {
            soilStorage.resetAll();
            setRecords([]);
            setSettings(DEFAULT_SETTINGS);
          }}
        />
      )}

      {/* Fixed 4-Tab Bottom Navigation matching Stitch HomeDashboard.html */}
      <nav aria-label="Primary navigation" className="fixed bottom-0 left-0 right-0 z-30 bg-light-surface-container-lowest rounded-t-lg clay-dock">
        <div className="max-w-5xl mx-auto grid grid-cols-4 h-16">
          {(
            [
              { id: 'home', label: 'Home', icon: 'home' },
              { id: 'scan', label: 'Scan', icon: 'photo_camera' },
              { id: 'advisor', label: 'AI Advisor', icon: 'psychology' },
              { id: 'reports', label: 'Reports', icon: 'description' },
            ] as const
          ).map((item) => {
            const active = tab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  if (item.id === 'scan') setScanSubView('hub');
                  setTab(item.id);
                }}
                aria-current={active ? 'page' : undefined}
                className={`flex flex-col items-center justify-center gap-0.5 ${
                  active
                    ? 'text-light-primary bg-light-secondary-container/40'
                    : 'text-light-on-surface-variant hover:text-light-primary'
                }`}
              >
                <span
                  className="material-symbols-outlined text-[22px]"
                  style={active ? { fontVariationSettings: "'FILL' 1" } : undefined}
                  aria-hidden="true"
                >
                  {item.icon}
                </span>
                <span className="font-label text-label-sm">{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      <BleCalibrationModal
        open={showBleModal}
        bleStatus={bleStatus}
        deviceName={bleDeviceName}
        lastReading={liveReading}
        bleError={bleError}
        settings={settings}
        onConnectBle={() => void connectBle()}
        onDisconnectBle={disconnectBle}
        onSaveSettings={(next) => {
          setSettings(next);
          soilStorage.saveSettings(next);
        }}
        onClose={() => setShowBleModal(false)}
      />
    </div>
  );
};
