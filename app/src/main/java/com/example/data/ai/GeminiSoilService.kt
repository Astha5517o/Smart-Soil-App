package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected soil image. Please choose a JPEG or PNG photo."))
        val promptText = """
            You are assisting FarmerBuddy (SMART SOIL), an AI-powered soil monitoring app.
            Analyze this soil photograph objectively in simple, clear, farmer-friendly English.
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
        val json = parseJsonObject(response.text)
        val possibleSoilType = json.optString("possibleSoilType").trim()
        val visibleCharacteristics = json.optString("visibleCharacteristics").trim()
        if (possibleSoilType.isEmpty() || visibleCharacteristics.isEmpty()) {
            throw IllegalStateException("Incomplete soil scan result. Please try a clearer photo.")
        }
        val testsArr = json.optJSONArray("recommendedAdditionalTests")
        val tests = if (testsArr != null) {
            (0 until testsArr.length()).mapNotNull { idx -> testsArr.optString(idx).trim().takeIf { it.isNotEmpty() } }
        } else emptyList()

        Result.success(
            SoilScanResult(
                visibleCharacteristics = visibleCharacteristics,
                possibleSoilType = possibleSoilType,
                organicMatterIndicators = json.optString("organicMatterIndicators").trim()
                    .ifEmpty { "No clear organic matter indicators visible." },
                uncertaintyExplanation = json.optString("uncertaintyExplanation").trim()
                    .ifEmpty { "A photo cannot measure soil pH, moisture %, or NPK nutrients." },
                recommendedAdditionalTests = tests.ifEmpty {
                    listOf("Check moisture with calibrated ESP32 probe", "Confirm pH and NPK via laboratory Soil Health Card")
                }
            )
        )
    }

    suspend fun identifyCropImage(imageUri: Uri): Result<CropIdResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected crop image. Please choose a JPEG or PNG photo."))
        val promptText = """
            Identify the crop in this photo for FarmerBuddy (SMART SOIL) in simple, clear English.
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
        val json = parseJsonObject(response.text)
        val cropName = json.optString("possibleCropName").trim()
        if (cropName.isEmpty()) {
            throw IllegalStateException("Could not identify crop from this photo. Try a clearer leaf image.")
        }
        Result.success(
            CropIdResult(
                possibleCropName = cropName,
                confidenceLevel = json.optString("confidenceLevel").trim().ifEmpty { "Moderate" },
                identifyingFeatures = json.optString("identifyingFeatures").trim()
                    .ifEmpty { "Visible leaf and stem pattern evaluated." },
                uncertaintyNotes = json.optString("uncertaintyNotes").trim()
                    .ifEmpty { "Please verify the crop match manually before saving." },
                suggestedGrowthStage = json.optString("suggestedGrowthStage").trim().ifEmpty { "Vegetative" }
            )
        )
    }

    suspend fun analyzePlantHealthImage(imageUri: Uri, selectedCrop: String): Result<PlantHealthResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read selected plant photo. Please choose a JPEG or PNG photo."))
        val promptText = """
            Examine this plant or leaf photograph for FarmerBuddy (current crop profile: $selectedCrop).
            Write in simple, clear, farmer-friendly English.
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
        val json = parseJsonObject(response.text)
        val symptoms = json.optString("visibleSymptoms").trim()
        if (symptoms.isEmpty()) {
            throw IllegalStateException("Incomplete plant health result. Please try a closer photo.")
        }
        val explanations = json.optJSONArray("possibleExplanations")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
        }.orEmpty().ifEmpty { listOf("Multiple nutrient, water, or leaf stress factors can appear similar.") }
        val checks = json.optJSONArray("suggestedChecks")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
        }.orEmpty().ifEmpty { listOf("Inspect underside of leaves and check soil moisture.") }
        val steps = json.optJSONArray("cautiousNextSteps")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
        }.orEmpty().ifEmpty { listOf("Consult a local agriculture officer before applying treatments.") }

        Result.success(
            PlantHealthResult(
                visibleSymptoms = symptoms,
                possibleExplanations = explanations,
                suggestedChecks = checks,
                cautiousNextSteps = steps,
                diagnosisLimitations = json.optString("diagnosisLimitations").trim()
                    .ifEmpty { "A photo alone cannot confirm a plant disease or exact deficiency." }
            )
        )
    }

    suspend fun extractNpkFromReportImage(imageUri: Uri): Result<NpkLabExtractionResult> = runSafeAi {
        val bitmap = decodeScaledBitmap(imageUri)
            ?: return@runSafeAi Result.failure(IllegalArgumentException("Could not read uploaded Soil Health Card or lab report image."))
        val promptText = """
            Extract Nitrogen (N), Phosphorus (P), and Potassium (K) values from this uploaded Soil Health Card or laboratory soil report for FarmerBuddy.
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
        val json = parseJsonObject(response.text)
        val n = json.optString("nitrogen").trim().ifEmpty { "Not readable" }
        val p = json.optString("phosphorus").trim().ifEmpty { "Not readable" }
        val k = json.optString("potassium").trim().ifEmpty { "Not readable" }
        val unreadable = isUnreadableField(n) && isUnreadableField(p) && isUnreadableField(k)

        Result.success(
            NpkLabExtractionResult(
                nitrogen = n,
                phosphorus = p,
                potassium = k,
                unit = json.optString("unit").trim().ifEmpty { "Not readable" },
                labSource = json.optString("labSource").trim().ifEmpty { "Not readable" },
                reportDate = json.optString("reportDate").trim().ifEmpty { "Not readable" },
                extractionNotes = json.optString("extractionNotes").trim().ifEmpty {
                    if (unreadable) "Could not clearly read N, P, K numbers. Please upload a sharper photo or enter values manually."
                    else "Verify all extracted numbers against your printed card before saving."
                },
                isUnreadable = unreadable
            )
        )
    }

    suspend fun fetchRegionalNpkReference(
        crop: String,
        region: String,
        soilType: String
    ): Result<NpkRegionalReferenceResult> = runSafeAi {
        val promptText = """
            Provide a cautious regional soil nutrient reference overview for FarmerBuddy:
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
        val json = parseJsonObject(response.text)
        Result.success(
            NpkRegionalReferenceResult(
                crop = crop,
                region = region,
                soilType = soilType.ifBlank { "Not specified" },
                sufficiencyStatus = json.optString("sufficiencyStatus").trim().ifEmpty { "Regional overview only" },
                nutrientOverview = json.optString("nutrientOverview").trim()
                    .ifEmpty { "Confirm actual soil nutrients using a local laboratory Soil Health Card." },
                source = json.optString("source").trim().ifEmpty { "General Agricultural Extension Reference" },
                referenceDate = json.optString("referenceDate").trim().ifEmpty { "General Reference" },
                limitations = json.optString("limitations").trim()
                    .ifEmpty { "Regional averages cannot replace a plot-specific laboratory soil test." },
                isInsufficientInfo = json.optBoolean("isInsufficientInfo", false)
            )
        )
    }

    suspend fun generateAdvisoryPlan(
        topic: String,
        userQuestion: String,
        contextSnapshot: String
    ): Result<AiAdvisoryPlan> = runSafeAi {
        val promptText = """
            You are the FarmerBuddy (SMART SOIL) Agronomy Advisor.
            Focus Topic: $topic
            User Question: ${userQuestion.ifBlank { "Provide practical soil & crop guidance based on the current readings and context." }}

            AVAILABLE CONTEXT:
            $contextSnapshot

            STRICT SCIENTIFIC RULES:
            - Consider moisture, pH, temperature, crop, growth stage, soil scan, and any NPK report/reference provided.
            - Distinguish DEMO data and REGIONAL REFERENCE ESTIMATES from LIVE ESP32 readings and VERIFIED LAB REPORTS.
            - Do NOT automatically recommend soil amendments (like lime or sulfur) from a single pH reading alone—advise verification and lab/buffer testing first.
            - Consider locally relevant organic/indigenous options (compost, farmyard manure, green manure, organic mulch, crop residues) only when appropriate.
            - NEVER invent fertilizer application quantities/dosages, guaranteed yield outcomes, or fake scientific citations.
            - Every recommendation item MUST include: identifiedIssue, whyItMatters, suggestedNextStep, supportingEvidence, limitationsAndUncertainty.
        """.trimIndent()

        val response = withTimeout(AI_TIMEOUT_MS) {
            advisorModel.generateContent(promptText)
        }
        val json = parseJsonObject(response.text)
        val summary = json.optString("summary").trim()
        val recArray = json.optJSONArray("recommendations")
            ?: throw IllegalStateException("Incomplete advisory recommendations.")
        val recs = (0 until recArray.length()).mapNotNull { idx ->
            val obj = recArray.optJSONObject(idx) ?: return@mapNotNull null
            AiRecommendationItem(
                category = obj.optString("category").trim().ifEmpty { "Soil Guidance" },
                identifiedIssue = obj.optString("identifiedIssue").trim().ifEmpty { "Condition review" },
                whyItMatters = obj.optString("whyItMatters").trim().ifEmpty { "Supports healthy root and crop growth." },
                suggestedNextStep = obj.optString("suggestedNextStep").trim().ifEmpty { "Verify plot readings before acting." },
                supportingEvidence = obj.optString("supportingEvidence").trim().ifEmpty { "Current sensor and crop context." },
                limitationsAndUncertainty = obj.optString("limitationsAndUncertainty").trim()
                    .ifEmpty { "Confirm with live probes and a Soil Health Card." }
            )
        }
        if (summary.isEmpty() || recs.isEmpty()) {
            throw IllegalStateException("Incomplete advisory response. Please try again.")
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

    private fun isUnreadableField(value: String): Boolean {
        val lower = value.trim().lowercase()
        return lower.isEmpty() ||
            lower == "not readable" ||
            lower == "unreadable" ||
            lower == "unclear" ||
            lower == "missing" ||
            lower == "n/a" ||
            lower == "unknown"
    }

    private fun parseJsonObject(rawText: String?): JSONObject {
        val text = rawText?.trim().orEmpty()
        if (text.isEmpty()) throw IllegalStateException("AI returned an empty response. Please try again.")
        val cleaned = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return JSONObject(cleaned)
    }

    private suspend fun <T> runSafeAi(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI request timed out after ${AI_TIMEOUT_MS / 1000}s", e)
            Result.failure(Exception("Scan timed out after 60 seconds. Check your internet connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic request failed", e)
            val rawMsg = e.localizedMessage.orEmpty()
            val friendlyMsg = when {
                rawMsg.contains("403", ignoreCase = true) || rawMsg.contains("appcheck", ignoreCase = true) ->
                    "App verification failed (HTTP 403). Make sure you have internet access and the latest app install."
                rawMsg.contains("429", ignoreCase = true) || rawMsg.contains("quota", ignoreCase = true) ->
                    "AI service is busy right now. Please wait a moment and try again."
                rawMsg.contains("Unable to resolve host", ignoreCase = true) ||
                    rawMsg.contains("network", ignoreCase = true) ||
                    rawMsg.contains("timeout", ignoreCase = true) ->
                    "No internet connection or slow network. Check connectivity and try again."
                rawMsg.isNotBlank() -> rawMsg
                else -> "AI analysis failed. Please check connectivity and try again."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    private suspend fun decodeScaledBitmap(uri: Uri, maxDim: Int = 1024): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

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
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@withContext null

                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                val scale = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
                if (scale < 1f || rotationDegrees != 0f) {
                    val matrix = Matrix().apply {
                        if (scale < 1f) postScale(scale, scale)
                        if (rotationDegrees != 0f) postRotate(rotationDegrees)
                    }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
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
