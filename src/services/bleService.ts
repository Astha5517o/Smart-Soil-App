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
    const warnings: string[] = [];

    // The current moisture-only ESP32 firmware sends a raw ADC integer as UTF-8 text.
    // Also accept JSON so expanded firmware can send moisture, pH and temperature together.
    if (/^\\d{1,4}$/.test(trimmed)) {
      const rawAdc = Number(trimmed);
      if (!Number.isInteger(rawAdc) || rawAdc < 0 || rawAdc > 4095) {
        return { ok: false, error: 'Invalid raw ADC packet (expected integer 0..4095).' };
      }
      return {
        ok: true,
        reading: {
          moistureRaw: rawAdc,
          moisturePercent: computeMoistureFromRaw(rawAdc, cal),
          ph: null,
          temperatureC: null,
          timestampMs: nowMs,
          isDemo: false,
          validationWarnings: [
            'This packet contains soil moisture only. pH and temperature will appear when the ESP32 firmware sends those sensor fields.',
          ],
        },
      };
    }

    if (!trimmed.startsWith('{')) {
      return { ok: false, error: 'Unsupported sensor packet. Expected a raw ADC integer or JSON object.' };
    }

    const data = JSON.parse(trimmed) as Record<string, unknown>;

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
    } else {
      const percentValue = data.moisture_percent ?? data.moisture;
      if (typeof percentValue === 'number' && Number.isFinite(percentValue)) {
        if (percentValue >= 0 && percentValue <= 100) {
          moisturePercent = Math.round(percentValue);
        } else {
          warnings.push('Invalid moisture percentage (expected 0..100).');
        }
      } else {
        warnings.push('Moisture reading unavailable.');
      }
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

    const temperatureValue = data.temperature_c ?? data.temperatureC ?? data.temperature ?? data.temp_c;
    let temperatureC: number | null = null;
    if (typeof temperatureValue === 'number' && Number.isFinite(temperatureValue)) {
      if (temperatureValue !== -127 && temperatureValue >= -20 && temperatureValue <= 85) {
        temperatureC = Math.round(temperatureValue * 10) / 10;
      } else {
        warnings.push(`Disconnected or invalid temperature sensor reading (${temperatureValue}°C).`);
      }
    } else {
      warnings.push('Temperature reading unavailable.');
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
      error: `Invalid BLE sensor packet: ${err instanceof Error ? err.message : 'Parse error'}`,
    };
  }
}
