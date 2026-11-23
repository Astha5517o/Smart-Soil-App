package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.AiAdvisoryPlan
import com.example.data.model.AiRecommendationItem
import com.example.data.model.CropIdResult
import com.example.data.model.NpkLabExtractionResult
import com.example.data.model.NpkRegionalReferenceResult
import com.example.data.model.PlantHealthResult
import com.example.data.model.SoilScanResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiSoilService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiSoilService(private val context: Context) {

    // Lazy models initialized only upon first inference to avoid startup crashes
    private val soilScanModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "visibleCharacteristics" to Schema.string("Visible texture, color, moisture look, clodding, and surface traits."),
                "possibleSoilType" to Schema.string("Possible broad soil type or texture class with cautious wording."),
                "organicMatterIndicators" to Schema.string("Visible crop residue, humus darkness, or root indicators."),
                "uncertaintyExplanation" to Schema.string("Clear explanation of why a photo cannot confirm chemistry or subsurface layers."),
                "recommendedAdditionalTests" to Schema.array(
                    items = Schema.string(),
                    description = "Useful physical, sensor, or laboratory follow-up tests."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val cropIdModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "possibleCropName" to Schema.string("Most likely crop name (common name)."),
                "confidenceLevel" to Schema.enumeration(listOf("High", "Moderate", "Low"), "Visual confidence level."),
                "identifyingFeatures" to Schema.string("Leaf shape, venation, stem, or canopy traits visible in photo."),
                "uncertaintyNotes" to Schema.string("Possible look-alike crops or reasons for visual uncertainty."),
                "suggestedGrowthStage" to Schema.enumeration(
                    listOf("Seedling", "Vegetative", "Flowering / Reproductive", "Maturity"),
                    "Estimated growth stage."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val plantHealthModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "visibleSymptoms" to Schema.string("Objective description of leaf spots, yellowing, wilting, or curling."),
                "possibleExplanations" to Schema.array(
                    items = Schema.string(),
                    description = "Non-definitive possible causes (nutrient stress, moisture stress, pest, fungal, etc.)."
                ),
                "suggestedChecks" to Schema.array(
                    items = Schema.string(),
                    description = "Field checks (underside of leaves, root moisture, soil pH probe, pattern across plot)."
                ),
                "cautiousNextSteps" to Schema.array(
                    items = Schema.string(),
                    description = "Safe, low-risk next steps without prescribing unverified chemicals."
                ),
                "diagnosisLimitations" to Schema.string("Explicit limitations of image-only diagnosis.")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val npkLabModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "nitrogen" to Schema.string("Readable Nitrogen (N) value, or 'Not readable' if missing."),
                "phosphorus" to Schema.string("Readable Phosphorus (P) value, or 'Not readable' if missing."),
                "potassium" to Schema.string("Readable Potassium (K) value, or 'Not readable' if missing."),
                "unit" to Schema.string("Measurement unit printed on report (e.g., kg/ha, ppm, mg/kg)."),
                "labSource" to Schema.string("Laboratory or Soil Health Card issuing body if readable."),
                "reportDate" to Schema.string("Date printed on report, or 'Not readable'."),
                "extractionNotes" to Schema.string("Notes on readability or fields the user should verify.")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val npkRegionalModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "sufficiencyStatus" to Schema.string("General regional reference summary without inventing measured field NPK numbers."),
                "nutrientOverview" to Schema.string("Qualitative N, P, K considerations from general agronomy extension references."),
                "source" to Schema.string("General agricultural reference source name (do not fabricate URLs or fake reports)."),
                "referenceDate" to Schema.string("Reference edition or general year if known, otherwise 'General Agronomy Reference'."),
                "limitations" to Schema.string("Why regional references cannot replace a site-specific laboratory soil test."),
                "isInsufficientInfo" to Schema.boolean("True if region/crop lacks reliable reference information.")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val advisorModel by lazy {
        val recSchema = Schema.obj(
            properties = mapOf(
                "category" to Schema.string("Topic such as Irrigation, pH Management, Organic Amendments, Nutrients, or Indigenous Practices."),
                "identifiedIssue" to Schema.string("Clear statement of the observed condition or question."),
                "whyItMatters" to Schema.string("Agronomic reason why this matters for the selected crop and stage."),
                "suggestedNextStep" to Schema.string("Practical, cautious next step without inventing exact chemical dosages."),
                "supportingEvidence" to Schema.string("Which sensor readings, scans, or agronomy principles support this."),
                "limitationsAndUncertainty" to Schema.string("Limitations, missing measurements, and uncertainty.")
            )
        )
        val planSchema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string("Concise 2-sentence overview tailored to the farmer or student."),
                "recommendations" to Schema.array(
                    items = recSchema,
                    description = "Structured recommendations, each with all 5 required elements."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            }
        )
    }

    suspend fun analyzeSoilImage(imageUri: Uri): Result<SoilScanResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected soil image."))
        val promptText = """
            You are assisting a school science exhibition project called SMART SOIL.
            Analyze this soil photograph objectively.
            STRICT SCIENTIFIC RULES:
            - Describe only visible characteristics (color, clodding, texture clues, visible organic matter).
            - Estimate a possible soil type cautiously.
            - NEVER claim that an image can measure soil pH, soil moisture percentage, or NPK nutrient levels.
            - Explain uncertainty clearly and suggest useful follow-up tests (e.g., ESP32 probe, jar texture test, lab NPK test).
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            soilScanModel.generateContent(
                content {
                    image(bitmap)
                    text(promptText)
                }
            )
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        val testsArr = json.getJSONArray("recommendedAdditionalTests")
        val tests = (0 until testsArr.length()).map { testsArr.getString(it) }
        Result.success(
            SoilScanResult(
                visibleCharacteristics = json.getString("visibleCharacteristics"),
                possibleSoilType = json.getString("possibleSoilType"),
                organicMatterIndicators = json.getString("organicMatterIndicators"),
                uncertaintyExplanation = json.getString("uncertaintyExplanation"),
                recommendedAdditionalTests = tests
            )
        )
    }

    suspend fun identifyCropImage(imageUri: Uri): Result<CropIdResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected crop image."))
        val promptText = """
            Identify the crop in this photo for SMART SOIL.
            Explain visible identifying features (leaf shape, arrangement, stem, panicle/fruit).
            State confidence level and uncertainty/look-alikes clearly so the user can confirm before saving.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            cropIdModel.generateContent(
                content {
                    image(bitmap)
                    text(promptText)
                }
            )
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        Result.success(
            CropIdResult(
                possibleCropName = json.getString("possibleCropName"),
                confidenceLevel = json.getString("confidenceLevel"),
                identifyingFeatures = json.getString("identifyingFeatures"),
                uncertaintyNotes = json.getString("uncertaintyNotes"),
                suggestedGrowthStage = json.getString("suggestedGrowthStage")
            )
        )
    }

    suspend fun analyzePlantHealthImage(imageUri: Uri, selectedCrop: String): Result<PlantHealthResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected plant photo."))
        val promptText = """
            Examine this plant or leaf photograph (current crop profile: $selectedCrop).
            STRICT SCIENTIFIC RULES:
            - Describe visible symptoms objectively.
            - List multiple possible explanations (nutrient deficiency, water stress, fungal/bacterial/viral, pest) without claiming a definitive disease diagnosis from an image alone.
            - Provide practical field checks and cautious next steps.
            - Clearly state limitations of image-based diagnosis.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            plantHealthModel.generateContent(
                content {
                    image(bitmap)
                    text(promptText)
                }
            )
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        val explanations = json.getJSONArray("possibleExplanations").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val checks = json.getJSONArray("suggestedChecks").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val steps = json.getJSONArray("cautiousNextSteps").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        Result.success(
            PlantHealthResult(
                visibleSymptoms = json.getString("visibleSymptoms"),
                possibleExplanations = explanations,
                suggestedChecks = checks,
                cautiousNextSteps = steps,
                diagnosisLimitations = json.getString("diagnosisLimitations")
            )
        )
    }

    suspend fun extractNpkFromReportImage(imageUri: Uri): Result<NpkLabExtractionResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read uploaded Soil Health Card or lab report image."))
        val promptText = """
            Extract Nitrogen (N), Phosphorus (P), and Potassium (K) values from this uploaded Soil Health Card or laboratory soil report.
            STRICT RULES:
            - Extract ONLY values that are actually readable on the document.
            - If a value, unit, date, or lab name is blurred or missing, output 'Not readable'.
            - NEVER invent or guess missing laboratory values.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            npkLabModel.generateContent(
                content {
                    image(bitmap)
                    text(promptText)
                }
            )
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        Result.success(
            NpkLabExtractionResult(
                nitrogen = json.getString("nitrogen"),
                phosphorus = json.getString("phosphorus"),
                potassium = json.getString("potassium"),
                unit = json.getString("unit"),
                labSource = json.getString("labSource"),
                reportDate = json.getString("reportDate"),
                extractionNotes = json.getString("extractionNotes")
            )
        )
    }

    suspend fun fetchRegionalNpkReference(
        crop: String,
        region: String,
        soilType: String
    ): Result<NpkRegionalReferenceResult> = runSafeAi {
        val promptText = """
            Provide a cautious regional soil nutrient reference overview for:
            Crop: $crop
            State / District / Region: $region
            Soil Type (if known): ${soilType.ifBlank { "Not specified" }}

            STRICT SCIENTIFIC RULES:
            - NEVER invent numerical field NPK test results.
            - NEVER represent crop nutrient uptake requirements as measured soil nutrient levels.
            - State clearly if reliable region-specific reference data is insufficient (set isInsufficientInfo = true).
            - Cite only general established agricultural extension concepts (e.g., ICAR / State Agricultural University Soil Health Card general guidance) without fabricating specific report numbers or URLs.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            npkRegionalModel.generateContent(promptText)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        Result.success(
            NpkRegionalReferenceResult(
                crop = crop,
                region = region,
                soilType = soilType.ifBlank { "Not specified" },
                sufficiencyStatus = json.getString("sufficiencyStatus"),
                nutrientOverview = json.getString("nutrientOverview"),
                source = json.getString("source"),
                referenceDate = json.getString("referenceDate"),
                limitations = json.getString("limitations"),
                isInsufficientInfo = json.getBoolean("isInsufficientInfo")
            )
        )
    }

    suspend fun generateAdvisoryPlan(
        topic: String,
        userQuestion: String,
        contextSnapshot: String
    ): Result<AiAdvisoryPlan> = runSafeAi {
        val promptText = """
            You are the SMART SOIL Agronomy Advisor for a school science exhibition.
            Focus Topic: $topic
            User Question: ${userQuestion.ifBlank { "Provide practical soil & crop guidance based on the current readings and context." }}

            AVAILABLE CONTEXT:
            $contextSnapshot

            STRICT SCIENTIFIC RULES:
            - Consider moisture, pH, temperature, crop, growth stage, soil scan, and any NPK report/reference provided.
            - Do NOT treat DEMO data as live field measurements; acknowledge if readings are Demo or Unavailable.
            - Do NOT automatically recommend soil amendments (like lime or sulfur) from a single pH reading alone—advise verification and lab/buffer testing first.
            - Consider locally relevant organic/indigenous options (compost, farmyard manure, green manure, organic mulch, crop residues) only when appropriate.
            - NEVER invent fertilizer application quantities/dosages, guaranteed yield outcomes, or fake scientific citations.
            - Every recommendation item MUST include: identifiedIssue, whyItMatters, suggestedNextStep, supportingEvidence, limitationsAndUncertainty.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            advisorModel.generateContent(promptText)
        }
        val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
        val summary = json.getString("summary")
        val recArray = json.getJSONArray("recommendations")
        val recs = (0 until recArray.length()).map { idx ->
            val obj = recArray.getJSONObject(idx)
            AiRecommendationItem(
                category = obj.getString("category"),
                identifiedIssue = obj.getString("identifiedIssue"),
                whyItMatters = obj.getString("whyItMatters"),
                suggestedNextStep = obj.getString("suggestedNextStep"),
                supportingEvidence = obj.getString("supportingEvidence"),
                limitationsAndUncertainty = obj.getString("limitationsAndUncertainty")
            )
        }
        Result.success(
            AiAdvisoryPlan(
                summary = summary,
                recommendations = recs,
                contextSnapshot = contextSnapshot,
                isDemo = false
            )
        )
    }

    private suspend fun <T> runSafeAi(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI request timed out after ${AI_TIMEOUT_MS / 1000}s", e)
            Result.failure(Exception("AI request timed out after 60 seconds. Check your internet connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic request failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI analysis failed. Please check connectivity."))
        }
    }

    private suspend fun decodeScaledBitmap(uri: Uri, maxDim: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                }
                val origW = boundsOptions.outWidth
                val origH = boundsOptions.outHeight
                if (origW <= 0 || origH <= 0) return@withContext null

                var sampleSize = 1
                while (origW / sampleSize > maxDim * 2 || origH / sampleSize > maxDim * 2) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@withContext null

                val scale = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
                if (scale < 1f) {
                    val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                        if (it != decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
                null
            }
        }
}
