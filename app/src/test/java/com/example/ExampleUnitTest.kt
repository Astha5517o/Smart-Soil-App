package com.example

import com.example.data.ble.SensorPayloadParser
import com.example.data.model.CalibrationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private val defaultCalibration = CalibrationConfig(dryAdcValue = 3200, wetAdcValue = 1400)

    @Test
    fun parseValidEsp32JsonPacket_computesCalibratedMoistureAndPreservesRaw() {
        val json = """{"moisture_raw":2300,"moisture_percent":50,"ph":6.4,"temperature_c":26.8,"timestamp":1790000000}"""
        val result = SensorPayloadParser.parseAndValidate(json, defaultCalibration, 1000L)

        assertTrue(result.isSuccess)
        val reading = result.getOrThrow()
        assertEquals(2300, reading.moistureRaw)
        assertEquals(50, reading.moisturePercent)
        assertEquals(6.4, reading.ph!!, 0.01)
        assertEquals(26.8, reading.temperatureC!!, 0.01)
        assertTrue(reading.validationErrors.isEmpty())
    }

    @Test
    fun parsePacketWithDisconnectedDs18b20AndUnverifiedPh_marksUnavailableWithoutFaking() {
        val json = """{"moisture_raw":2408,"ph":null,"temperature_c":-127.0}"""
        val result = SensorPayloadParser.parseAndValidate(json, defaultCalibration, 1000L)

        assertTrue(result.isSuccess)
        val reading = result.getOrThrow()
        assertEquals(44, reading.moisturePercent)
        assertNull(reading.ph)
        assertNull(reading.temperatureC)
        assertEquals(2, reading.validationErrors.size)
    }

    @Test
    fun parsePacketWithImpossibleValues_rejectsOutOfRangeReadings() {
        val json = """{"moisture_raw":9999,"ph":19.5,"temperature_c":250.0}"""
        val result = SensorPayloadParser.parseAndValidate(json, defaultCalibration, 1000L)

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun parseMalformedPacket_failsCleanly() {
        val result = SensorPayloadParser.parseAndValidate("not-json", defaultCalibration, 1000L)
        assertTrue(result.isFailure)
    }
}
