package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.AppDatabase
import com.example.data.local.SoilRecordEntity
import com.example.data.model.RecordCategory
import com.example.data.model.RecordOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches and displays home dashboard`() {
        composeRule.onNodeWithText("FarmerBuddy").assertIsDisplayed()
        composeRule.onNodeWithTag("home_start_scan_button").assertIsDisplayed()
        composeRule.onNodeWithText("SCAN YOUR SOIL").assertIsDisplayed()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FarmerBuddy", appName)
    }

    @Test
    fun `room database saves and clears demo records cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.soilRecordDao()

        dao.insertRecord(
            SoilRecordEntity(
                id = "live-1",
                category = RecordCategory.SENSOR_READING.name,
                originBadge = RecordOrigin.LIVE_SENSOR.name,
                isDemo = false,
                dataSource = "ESP32 BLE",
                title = "Live Reading",
                summary = "Moisture 44%",
                fullDetails = "Live"
            )
        )
        dao.insertRecord(
            SoilRecordEntity(
                id = "demo-1",
                category = RecordCategory.SENSOR_READING.name,
                originBadge = RecordOrigin.DEMO_DATA.name,
                isDemo = true,
                dataSource = "Demo Mode",
                title = "Demo Reading",
                summary = "Moisture 24%",
                fullDetails = "Demo"
            )
        )

        assertEquals(2, dao.observeAllRecords().first().size)
        dao.deleteDemoRecords()
        val remaining = dao.observeAllRecords().first()
        assertEquals(1, remaining.size)
        assertTrue(!remaining.first().isDemo)
        db.close()
    }
}
