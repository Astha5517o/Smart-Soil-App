import type { CalibrationConfig, SensorReading } from '../types/soil';

export function isWebBluetoothSupported(): boolean {
  return typeof navigator !== 'undefined' && typeof navigator.bluetooth !== 'undefined';
}

export function computeMoistureFromRaw(rawAdc: number, cal: CalibrationConfig): number | null {
  if (cal.dryAdc === cal.wetAdc) return null;
  const pct = ((cal.dryAdc - rawAdc) / (cal.dryAdc - cal.wetAdc)) * 100;
  return Math.max(0, Math.min(100, Math.round(pct)));
}

export function parseAndValidateSensorJson(
  rawText: string,
  cal: CalibrationConfig,
  nowMs: number = Date.now()
): { ok: true; reading: SensorReading } | { ok: false; error: string } {
  try {
    const trimmed = rawText.trim();
    if (!trimmed.startsWith('{')) {
      return { ok: false, error: 'Malformed sensor packet (not a JSON object).' };
    }
    const data = JSON.parse(trimmed) as Record<string, unknown>;
    const warnings: string[] = [];

    let moistureRaw: number | null = null;
    if (typeof data.moisture_raw === 'number' && Number.isFinite(data.moisture_raw)) {
      if (data.moisture_raw >= 0 && data.moisture_raw <= 4095) {
        moistureRaw = Math.round(data.moisture_raw);
      } else {
        warnings.push('Invalid moisture_raw ADC value (expected 0..4095).');
      }
    }

    let moisturePercent: number | null = null;
    if (moistureRaw !== null) {
      moisturePercent = computeMoistureFromRaw(moistureRaw, cal);
    } else if (typeof data.moisture_percent === 'number' && Number.isFinite(data.moisture_percent)) {
      if (data.moisture_percent >= 0 && data.moisture_percent <= 100) {
        moisturePercent = Math.round(data.moisture_percent);
      } else {
        warnings.push('Invalid moisture_percent (expected 0..100).');
      }
    } else {
      warnings.push('Moisture reading unavailable.');
    }

    let ph: number | null = null;
    if (typeof data.ph === 'number' && Number.isFinite(data.ph)) {
      if (data.ph >= 0 && data.ph <= 14) {
        ph = Math.round(data.ph * 100) / 100;
      } else {
        warnings.push('Invalid pH value (expected 0..14).');
      }
    } else {
      warnings.push('Soil pH reading unavailable or unverified.');
    }

    let temperatureC: number | null = null;
    if (typeof data.temperature_c === 'number' && Number.isFinite(data.temperature_c)) {
      if (data.temperature_c !== -127 && data.temperature_c >= -20 && data.temperature_c <= 85) {
        temperatureC = Math.round(data.temperature_c * 10) / 10;
      } else {
        warnings.push(`Disconnected or invalid DS18B20 temperature (${data.temperature_c}°C).`);
      }
    } else {
      warnings.push('DS18B20 temperature reading unavailable.');
    }

    if (moistureRaw === null && moisturePercent === null && ph === null && temperatureC === null) {
      return { ok: false, error: warnings[0] || 'Packet contained no valid sensor values.' };
    }

    return {
      ok: true,
      reading: {
        moistureRaw,
        moisturePercent,
        ph,
        temperatureC,
        timestampMs: nowMs,
        isDemo: false,
        validationWarnings: warnings,
      },
    };
  } catch (err) {
    return {
      ok: false,
      error: `Invalid BLE JSON packet: ${err instanceof Error ? err.message : 'Parse error'}`,
    };
  }
}
