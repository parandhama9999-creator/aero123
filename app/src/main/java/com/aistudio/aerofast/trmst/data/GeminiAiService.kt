package com.aistudio.aerofast.trmst.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aistudio.aerofast.trmst.config.AppConfig
import com.aistudio.aerofast.trmst.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

data class DetectedDimension(
    val feature: String,
    val nominal: String,
    val tolerance: String
)

data class ExtractedFastenerSpec(
    val fastenerType: FastenerType,
    val unitSystem: UnitSystem,
    val nominalSizeIndex: Int,
    val nominalDiaMm: Double,
    val pitchValue: Double,
    val lengthMm: Double,
    val propertyClass: PropertyClass,
    val materialKey: String,
    val threadClass: String,
    val coating: CoatingType,
    val technicalSummary: String,
    val detectedDimensions: List<DetectedDimension>,
    val isAiVisionExtracted: Boolean,
    val modelUsed: String = "gemini-3.5-flash",
    val radius: Double? = null,
    // Drawing geometry for volume / pre-extrusion length (null when the drawing does not show it), all in mm
    val threadLengthMm: Double? = null,
    val headAcrossFlatsMm: Double? = null,
    val headHeightMm: Double? = null,
    val flangeDiaMm: Double? = null,
    val flangeThicknessMm: Double? = null,
    val shankDiaMm: Double? = null,
    val headDiaMm: Double? = null,
    val socketSizeMm: Double? = null,
    val socketDepthMm: Double? = null,
    val neckHeightMm: Double? = null,
    val nutHeightMm: Double? = null,
    val washerOuterDiaMm: Double? = null,
    val washerInnerDiaMm: Double? = null,
    val washerThicknessMm: Double? = null,
    /** Pan head crown radius rf and cylindrical edge height. */
    val headDomeRadiusMm: Double? = null,
    val headEdgeHeightMm: Double? = null,
    /** Tensile band from the drawing notes, when it is given instead of a property class. */
    val tensileMinMpa: Double? = null,
    val tensileMaxMpa: Double? = null,
    /** Nylon thread-locking patch / pellet called for on the drawing. */
    val threadLockPatch: Boolean = false,
    /** Order quantity from the drawing's title block (QTY), when shown. */
    val batchQty: Int? = null,
    /** Values the drawing did not show, so a default stands in: each entry starts with the value's name. */
    val assumed: List<String> = emptyList(),
    /** The part as a stack of solids from the head end to the point end, as the AI read it (empty when it gave none). */
    val solids: List<DrawnSolid> = emptyList(),
    val cutouts: List<DrawnCutout> = emptyList()
) {
    /** Every drawing dimension the volume model can use. */
    fun geometry(): DrawingGeometry {
        val roundHead = PartGeometry.styleOf(fastenerType) in setOf(
            HeadStyle.SOCKET_CAP, HeadStyle.SOCKET_COUNTERSUNK, HeadStyle.SOCKET_BUTTON, HeadStyle.CARRIAGE, HeadStyle.ROUND_PIN, HeadStyle.RIVET
        )
        return DrawingGeometry(
            threadLengthMm = threadLengthMm, shankDiaMm = shankDiaMm, acrossFlatsMm = headAcrossFlatsMm,
            // A round head's diameter is sometimes reported in the across-flats field
            headDiaMm = headDiaMm ?: headAcrossFlatsMm.takeIf { roundHead },
            headHeightMm = headHeightMm, flangeDiaMm = flangeDiaMm, flangeThicknessMm = flangeThicknessMm,
            socketSizeMm = socketSizeMm, socketDepthMm = socketDepthMm, neckHeightMm = neckHeightMm, nutHeightMm = nutHeightMm,
            washerOuterDiaMm = washerOuterDiaMm, washerInnerDiaMm = washerInnerDiaMm, washerThicknessMm = washerThicknessMm,
            headDomeRadiusMm = headDomeRadiusMm, headEdgeHeightMm = headEdgeHeightMm
        )
    }

    val tensileRange: TensileRange? get() = TensileRange.of(tensileMinMpa, tensileMaxMpa)
}

object GeminiAiService {
    // Production models with multimodal vision support
    val MODEL_FALLBACK_CHAIN = listOf(
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-3.1-flash-lite-preview",
        // Auto-updating alias as a last resort, so the app keeps working when the named models above are retired
        "gemini-flash-lite-latest"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Rate limiting: max 1 request per 500ms
    @Volatile private var lastApiCallTime = 0L
    private val API_CALL_MIN_INTERVAL = 500L

    private suspend fun checkRateLimit() {
        val now = System.currentTimeMillis()
        val timeSinceLastCall = now - lastApiCallTime
        if (timeSinceLastCall < API_CALL_MIN_INTERVAL) {
            kotlinx.coroutines.delay(API_CALL_MIN_INTERVAL - timeSinceLastCall)
        }
        lastApiCallTime = System.currentTimeMillis()
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return (this * multiplier).roundToInt() / multiplier
    }

    // High quality compression preserving color finishes and fine drawing lines for Gemini Vision OCR
    fun compressBitmapForAi(bitmap: Bitmap, maxDim: Int = 1600, quality: Int = 85): Bitmap {
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) {
            return bitmap
        }
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = compressBitmapForAi(bitmap, 1600, 85)
        val outputStream = ByteArrayOutputStream(256 * 1024)
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val result = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        if (scaled !== bitmap) scaled.recycle()
        return result
    }

    // 1. Robust Image CRC32 Hash / Fingerprint for LocalStorage Cache (avoids collisions)
    private fun getImageFingerprint(bitmap: Bitmap): String {
        val crc = java.util.zip.CRC32()
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)
        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                crc.update(bitmap.getPixel(x, y))
            }
        }
        return "w${bitmap.width}_h${bitmap.height}_crc${crc.value}"
    }

    /** The encrypted store, for other modules that keep secrets (AI provider keys). */
    internal fun encryptedPrefs(context: Context, name: String): EncryptedSharedPreferences = getEncryptedPrefs(context, name)

    private fun getEncryptedPrefs(context: Context, name: String): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(context, name, masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM) as EncryptedSharedPreferences
    }

    private fun getCachedExtraction(context: Context, fingerprint: String): ExtractedFastenerSpec? {
        return try {
            val prefs = getEncryptedPrefs(context, AppConfig.SCAN_CACHE_ENCRYPTED_NAME)
            val cachedData = prefs.getString(fingerprint, null) ?: return null
            val json = JSONObject(cachedData)
            val timestamp = json.optLong("_timestamp", 0L)
            val now = System.currentTimeMillis()
            val CACHE_VALIDITY_MS = 30L * 24L * 60L * 60L * 1000L // 30 days
            if (now - timestamp > CACHE_VALIDITY_MS) {
                prefs.edit().remove(fingerprint).apply() // Expired, remove
                return null
            }
            val spec = json.optJSONObject("_spec") ?: return null
            parseSpecFromJson(spec, "Cached (Zero API)")
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheExtraction(context: Context, fingerprint: String, rawJsonText: String) {
        try {
            val prefs = getEncryptedPrefs(context, AppConfig.SCAN_CACHE_ENCRYPTED_NAME)
            val wrapper = JSONObject()
                .put("_timestamp", System.currentTimeMillis())
                .put("_spec", JSONObject(rawJsonText))
            prefs.edit().putString(fingerprint, wrapper.toString()).apply()
        } catch (e: Exception) {
            // Silent fail - caching is optional
        }
    }

    /**
     * Reads the key a build before the filename fix left in the plaintext-shared "aerofast_prefs"
     * file and moves it into the secure store, so an existing user does not have to retype it.
     * Returns the migrated key, or "" when there is nothing to migrate (the usual case).
     */
    private fun migrateLegacyApiKey(context: Context): String {
        return try {
            val legacy = getEncryptedPrefs(context, AppConfig.PREFERENCES_LEGACY_NAME)
            val key = (legacy.getString(AppConfig.KEY_GEMINI_PRIMARY, null)
                ?: legacy.getString(AppConfig.KEY_GEMINI_LEGACY, null))?.trim().orEmpty()
            if (key.isNotBlank()) {
                saveUserApiKey(context, key)
                legacy.edit()
                    .remove(AppConfig.KEY_GEMINI_PRIMARY)
                    .remove(AppConfig.KEY_GEMINI_LEGACY)
                    .apply()
            }
            key
        } catch (e: Exception) {
            // The legacy file is plaintext XML that the encrypted reader cannot parse, or its master
            // key did not survive a device-to-device restore. Nothing to migrate: the user retypes it.
            ""
        }
    }

    fun getEffectiveApiKey(context: Context): String {
        return try {
            val prefs = getEncryptedPrefs(context, AppConfig.PREFERENCES_ENCRYPTED_NAME)
            val key = (prefs.getString(AppConfig.KEY_GEMINI_PRIMARY, null)
                ?: prefs.getString(AppConfig.KEY_GEMINI_LEGACY, null))?.trim().orEmpty()
            if (key.isBlank()) migrateLegacyApiKey(context) else key
        } catch (e: Exception) {
            ""
        }
    }

    fun hasLiveApiKey(context: Context): Boolean {
        val key = getEffectiveApiKey(context)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "PASTE_YOUR_ACTUAL_API_KEY_HERE"
    }

    fun saveUserApiKey(context: Context, key: String) {
        try {
            val prefs = getEncryptedPrefs(context, AppConfig.PREFERENCES_ENCRYPTED_NAME)
            prefs.edit()
                .putString(AppConfig.KEY_GEMINI_PRIMARY, key.trim())
                .putString(AppConfig.KEY_GEMINI_LEGACY, key.trim())
                .apply()
        } catch (e: Exception) {
            throw RuntimeException("Failed to save API key securely: ${e.message}", e)
        }
    }

    /** The Sizing screen's forging results and die stations, so the Co-Pilot explains the same numbers the user sees. */
    fun forgingContext(r: FastenerCalculationResult): String = buildString {
        val f = r.boltForging
        if (f != null) {
            append("Forging (same values as the Sizing card): thread length b ${"%.1f".format(java.util.Locale.US, f.threadLengthMm)} mm; ")
            append("shank Ø${"%.2f".format(java.util.Locale.US, f.shankDiaMm)} extruded to blank Ø${"%.3f".format(java.util.Locale.US, f.blankDiaMm)} through a 15°/side cone ${"%.2f".format(java.util.Locale.US, f.coneLengthMm)} mm; ")
            append("stock before extrusion ${"%.2f".format(java.util.Locale.US, f.stockToExtrudeMm)} mm; length under head before extrusion ${"%.2f".format(java.util.Locale.US, f.lengthUnderHeadBeforeMm)} mm; ")
            f.finishedVolumeMm3?.let { append("finished volume ${"%.0f".format(java.util.Locale.US, it)} mm³; ") }
            f.cheeseVolumeMm3?.let { append("trim cheese ${"%.0f".format(java.util.Locale.US, it)} mm³; ") }
            f.headStockMm?.let { append("head stock ${"%.2f".format(java.util.Locale.US, it)} mm; ") }
            f.cutOffLengthMm?.let { append("cut-off ${"%.2f".format(java.util.Locale.US, it)} mm") }
            append(". ${f.headNote}.\n")
        }
        r.headingPlan?.let { plan ->
            append("Die progression (${plan.stations.size} stations, slug Ø${"%.2f".format(java.util.Locale.US, plan.slugDiaMm)} × ${"%.2f".format(java.util.Locale.US, plan.slugLengthMm)} mm):\n")
            plan.stations.forEach { append("- ${it.index}. ${it.title}: ${it.operation}\n") }
        }
    }

    /** What the vision model is asked for: every dimension, and the part as a stack of solids so its whole volume can be computed. */
    fun buildExtractionPrompt(): String = """
        Fastener drawing inspection. Read every callout, title block and note. Return ONLY this JSON:
        {"nominalDiameterMm":number,"pitch":number,"lengthMm":number,"fastenerType":"","unitSystem":"METRIC|INCH",
         "materialKey":"","propertyClass":"","threadClass":"","coating":"",
         "threadLengthMm":number,"shankDiaMm":number,"headAcrossFlatsMm":number,"headDiaMm":number,"headHeightMm":number,
         "flangeDiaMm":number,"flangeThicknessMm":number,"socketSizeMm":number,"socketDepthMm":number,"neckHeightMm":number,
         "nutHeightMm":number,"washerOuterDiaMm":number,"washerInnerDiaMm":number,"washerThicknessMm":number,"radius":number,
         "headDomeRadiusMm":number,"headEdgeHeightMm":number,"tensileMinMpa":number,"tensileMaxMpa":number,"threadLockPatch":false,"quantity":number,
         "solids":[{"feature":"","shape":"CYLINDER|CONE|HEX|SQUARE|TWELVE_POINT|DOME","d1":number,"d2":number,"lengthMm":number,"threaded":false}],
         "cutouts":[{"feature":"","shape":"CYLINDER|CONE|HEX|SQUARE|TWELVE_POINT","d1":number,"d2":number,"lengthMm":number,"count":1,"from":"HEAD_END|POINT_END|THROUGH|CROSS","threaded":false}],
         "detectedDimensions":[{"feature":"","nominal":"","tolerance":""}],"summary":""}
        Rules:
        - fastenerType one of: ${FastenerType.values().joinToString(",") { it.name }}
        - coating one of: ${CoatingType.values().joinToString(",") { it.name }}. Cadmium per QQ-P-416: AERO_CAD = Class 1, AERO_CAD_CL2 = Class 2, AERO_CAD_CL3 = Class 3.
        - A pan head with a hexalobular (Torx) recess is PAN_HEXALOBULAR. headDomeRadiusMm = crown radius R of a pan head dome; headEdgeHeightMm = height of the pan head's cylindrical edge under the dome.
        - tensileMinMpa / tensileMaxMpa = tensile strength from the notes (e.g. "TENSILE STRENGTH = 1000-1240MPa"; ksi × 6.895). threadLockPatch = true when a nylon patch, pellet or locking element is called for. quantity = order quantity (QTY) in the title block or parts list, 0 if not shown.
        - materialKey e.g. 10B21, 4140, SS316, A286, Ti-6Al-4V; propertyClass e.g. 8.8, 10.9, A2-70; threadClass e.g. 6g, 6H, 2A, 3A.
        - Inch drawings: give every diameter and length in mm and pitch in mm (25.4/TPI).
        - threadLengthMm = thread length b; shankDiaMm = plain shank / body diameter; headAcrossFlatsMm = across flats s of a hex or 12-point head, or of a nut;
          headDiaMm = diameter of a round head (socket cap, countersunk, button, carriage, pin, rivet); headHeightMm = head height k (incl. flange);
          flangeDiaMm / flangeThicknessMm = flange or washer-face dc and c; socketSizeMm / socketDepthMm = drive socket key size and depth;
          neckHeightMm = square neck height; nutHeightMm = nut height m; washerOuterDiaMm / washerInnerDiaMm / washerThicknessMm = washer outside Ø, bore Ø, thickness;
          radius = under-head fillet radius. All in mm, 0 if not shown.
        - solids: the finished part as a stack of solids along its axis, in order from the head (drive) end to the point end. They must cover the whole
          part once, with no gaps and no overlaps, so their lengthMm add up to the overall length including the head. One entry for each section that
          has its own diameter or shape: head, flange / collar / washer face, neck, plain shank, shoulder, groove or undercut, thread, dog point.
          CYLINDER: d1 = diameter. CONE (countersunk head, taper, chamfer): d1 = diameter at its head-side end, d2 = at its point-side end.
          HEX / TWELVE_POINT: d1 = across flats. SQUARE: d1 = side. DOME (button, mushroom or rivet head): d1 = base diameter, lengthMm = dome height.
          threaded = true for a threaded length (d1 = nominal thread diameter). A nut or a washer is one solid; its hole goes in cutouts.
        - cutouts: material removed from those solids: drive socket, nut bore, washer hole, drilled or cross holes, slots. d1 = diameter (across flats of a
          hex socket), lengthMm = depth (the full length for a through hole), count = how many, from = where it enters, threaded = true for a tapped hole.
        - solids and cutouts use only dimensions shown on the drawing. If a section's size is not dimensioned, return "solids":[] instead of guessing.
        - detectedDimensions: EVERY dimension on the drawing, one entry each, nominal with unit, tolerance exactly as written ("" if none):
          diameters, lengths, thread length, head height, across flats/corners, flange/socket sizes, chamfers, radii, angles, GD&T callouts.
        - Only values actually shown; never invent. Use "" (or 0 for numbers) when not shown.
        - summary: one sentence.
    """.trimIndent()

    suspend fun extractBlueprintSpecFromImage(
        imageBitmap: Bitmap,
        context: Context,
        bypassCache: Boolean = false
    ): Result<ExtractedFastenerSpec> = withContext(Dispatchers.IO) {
        val fingerprint = getImageFingerprint(imageBitmap)
        if (!bypassCache) {
            val cachedSpec = getCachedExtraction(context, fingerprint)
            if (cachedSpec != null && !cachedSpec.modelUsed.contains("Offline") && !cachedSpec.modelUsed.contains("Built-in")) {
                return@withContext Result.success(cachedSpec)
            }
        }

        val apiKey = getEffectiveApiKey(context)
        val provider = AiSettings.load(context)
        if (!AiSettings.isConfigured(context)) {
            // Without a key nothing reads the drawing: never show invented values as "detected"
            return@withContext Result.failure(IllegalStateException(
                "No AI provider set up: the drawing was NOT read. Tap the Key button, add a Gemini key (or a vision-capable provider), then analyze again. " +
                    "(The sample drawings above work without a key.)"))
        }

        val base64Image = bitmapToBase64(imageBitmap)
        val extractionPrompt = buildExtractionPrompt()

        if (provider.provider != AiProvider.GEMINI) {
            // Another provider reads the drawing only if its model takes images; its own error is passed on otherwise
            return@withContext OpenAiCompatClient.chat(provider, extractionPrompt, base64Image, maxTokens = 8192, temperature = 0.1).mapCatching { raw ->
                var text = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val a = text.indexOf('{'); val b = text.lastIndexOf('}')
                if (a == -1 || b <= a) throw IllegalStateException("${provider.provider.label} (${provider.model}) did not return drawing data. Choose a vision model or use Gemini.")
                text = text.substring(a, b + 1)
                val spec = parseSpecFromJson(JSONObject(text), "${provider.provider.label}: ${provider.model}")
                cacheExtraction(context, fingerprint, text)
                spec
            }
        }

        fun buildJsonBody(): JSONObject {
            return JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().put("text", extractionPrompt))
                    partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }))
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    // Room for thinking plus a long dimension list; 2048 could truncate the JSON mid-way
                    put("maxOutputTokens", 8192)
                    put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal"))
                    put("responseMimeType", "application/json")
                })
            }
        }

        var lastError: String? = null
        for (model in MODEL_FALLBACK_CHAIN) {
            try {
                checkRateLimit() // Enforce rate limiting
                val jsonBody = buildJsonBody()
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                    .header("User-Agent", "AeroFastPro/1.0")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = httpClient.newCall(request).execute()
                val responseBody = response.use { it.body?.string() ?: "" }
                if (!response.isSuccessful) {
                    lastError = describeHttpError(model, response.code, responseBody)
                    continue
                }

                val rootJson = JSONObject(responseBody)

                // Validate response structure
                if (!rootJson.has("candidates")) {
                    lastError = "Invalid API response: missing 'candidates' field"
                    continue
                }

                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    lastError = "Model $model returned no candidates"
                    continue
                }

                val candidate = candidates.optJSONObject(0) ?: run {
                    lastError = "Invalid candidate object"
                    continue
                }

                val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    lastError = "Model $model returned no response parts"
                    continue
                }

                var text = ""
                for (p in 0 until parts.length()) {
                    val partObj = parts.optJSONObject(p) ?: continue
                    val isThought = partObj.optBoolean("thought", false)
                    if (!isThought) {
                        val pText = partObj.optString("text", "").trim()
                        if (pText.isNotBlank()) {
                            text += pText
                        }
                    }
                }

                if (text.isBlank()) {
                    lastError = "Model $model returned empty response text"
                    continue
                }

                if (text.startsWith("```json")) text = text.removePrefix("```json").trim()
                if (text.startsWith("```")) text = text.removePrefix("```").trim()
                if (text.endsWith("```")) text = text.removeSuffix("```").trim()
                val firstBrace = text.indexOf('{')
                val lastBrace = text.lastIndexOf('}')
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    text = text.substring(firstBrace, lastBrace + 1)
                }

                val parsedJson = JSONObject(text)
                val spec = parseSpecFromJson(parsedJson, model)
                cacheExtraction(context, fingerprint, text)
                return@withContext Result.success(spec)
            } catch (e: Exception) {
                lastError = e.localizedMessage
            }
        }

        // Every model failed: report the real reason instead of showing an invented spec as if it were read from the drawing
        Result.failure(Exception("AI scan failed: ${lastError ?: "no response from Gemini"}. Check your internet connection or set a valid Gemini API key (Key button)."))
    }

    private fun describeHttpError(model: String, code: Int, body: String): String {
        val apiMessage = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) { "" }
        return when (code) {
            400, 401, 403 -> "Gemini rejected the request (HTTP $code)" + if (apiMessage.isNotBlank()) ": ${apiMessage.take(160)}" else ""
            429 -> "Gemini quota exceeded (HTTP 429)"
            else -> "Model $model returned HTTP $code" + if (apiMessage.isNotBlank()) ": ${apiMessage.take(160)}" else ""
        }
    }

    /** A number the drawing actually showed (above zero), from a JSON number or a string such as "12.5 mm". */
    private fun shown(json: JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        val num = json.optDouble(key, Double.NaN)
        if (!num.isNaN()) return num.takeIf { it > 0.0 }
        return Regex("""([0-9]+(?:\.[0-9]+)?)""").find(json.optString(key, ""))?.value?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun parseSolids(json: JSONObject): Pair<List<DrawnSolid>, List<DrawnCutout>> {
        fun shape(o: JSONObject) = runCatching { SolidShape.valueOf(o.optString("shape", "").trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')) }.getOrNull()
        val solids = mutableListOf<DrawnSolid>()
        val arr = json.optJSONArray("solids") ?: return emptyList<DrawnSolid>() to emptyList()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: return emptyList<DrawnSolid>() to emptyList()
            // One unreadable section means the stack no longer covers the part: use none of it
            val sh = shape(o) ?: return emptyList<DrawnSolid>() to emptyList()
            val d1 = shown(o, "d1") ?: return emptyList<DrawnSolid>() to emptyList()
            val len = shown(o, "lengthMm") ?: return emptyList<DrawnSolid>() to emptyList()
            solids += DrawnSolid(o.optString("feature", "").trim(), sh, d1, shown(o, "d2") ?: 0.0, len, o.optBoolean("threaded", false))
        }
        val cutouts = mutableListOf<DrawnCutout>()
        val cuts = json.optJSONArray("cutouts")
        if (cuts != null) for (i in 0 until cuts.length()) {
            val o = cuts.optJSONObject(i) ?: continue
            val sh = shape(o)?.takeIf { it != SolidShape.DOME } ?: return emptyList<DrawnSolid>() to emptyList()
            val d1 = shown(o, "d1") ?: return emptyList<DrawnSolid>() to emptyList()
            val len = shown(o, "lengthMm") ?: return emptyList<DrawnSolid>() to emptyList()
            val from = runCatching { CutoutFrom.valueOf(o.optString("from", "HEAD_END").trim().uppercase().replace(Regex("[^A-Z]+"), "_").trim('_')) }
                .getOrDefault(CutoutFrom.HEAD_END)
            cutouts += DrawnCutout(o.optString("feature", "").trim(), sh, d1, shown(o, "d2") ?: 0.0, len, o.optInt("count", 1).coerceAtLeast(1), from, o.optBoolean("threaded", false))
        }
        return solids to cutouts
    }

    fun parseSpecFromJson(parsedJson: JSONObject, modelName: String): ExtractedFastenerSpec {
        val assumed = mutableListOf<String>()
        // Normalise "Hex Bolt" / "hex-bolt" to HEX_BOLT so any of the app's fastener types can be recognised
        fun normEnum(s: String) = s.trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
        val rawType = normEnum(parsedJson.optString("fastenerType", ""))
        val fastenerType = FastenerType.values().find { it.name == rawType }
            ?: FastenerType.values().find { normEnum(it.label) == rawType }
            ?: (if (rawType.contains("NUT")) FastenerType.HEX_NUT else FastenerType.HEX_BOLT).also {
                assumed += "fastener type (${if (rawType.isBlank()) "not on the drawing" else "'$rawType' not recognised"}: ${it.label.substringBefore(" (")} used)"
            }
        val rawUnit = parsedJson.optString("unitSystem", "METRIC")
        val unitSystem = if (rawUnit.uppercase().contains("INCH")) UnitSystem.INCH else UnitSystem.METRIC
        // A value the drawing does not show keeps a default so the app can still calculate, but it is reported, never passed off as read
        val extractedDia = shown(parsedJson, "nominalDiameterMm") ?: 10.0.also { assumed += "nominal diameter (not on the drawing: 10 mm used)" }
        val extractedLength = shown(parsedJson, "lengthMm") ?: 50.0.also {
            if (!fastenerType.isNut && !fastenerType.isWasher) assumed += "length (not on the drawing: 50 mm used)"
        }

        val sizeIndex: Int = if (unitSystem == UnitSystem.METRIC) {
            var closestIdx = 0
            var minDiff = Double.MAX_VALUE
            FastenerCatalog.METRIC_SIZES.forEachIndexed { index, metric ->
                val diff = abs(metric.d - extractedDia)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIdx = index
                }
            }
            closestIdx
        } else {
            var closestIdx = 0
            var minDiff = Double.MAX_VALUE
            FastenerCatalog.INCH_SIZES.forEachIndexed { index, inch ->
                val diff = abs((inch.dInch * 25.4) - extractedDia)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIdx = index
                }
            }
            closestIdx
        }
        // No pitch on the drawing: the coarse pitch of that size, not one fixed value for every diameter
        val coarsePitchMm = if (unitSystem == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[sizeIndex].pitches.first()
            else 25.4 / FastenerCatalog.INCH_SIZES[sizeIndex].tpis.first()
        val extractedPitch = shown(parsedJson, "pitch") ?: coarsePitchMm.also {
            if (!fastenerType.isUnthreaded && !fastenerType.isWasher) assumed += "pitch (not on the drawing: coarse pitch ${it.round(3)} mm used)"
        }

        val rawMat = parsedJson.optString("materialKey", "").uppercase()
        val matchedMaterialKey = if (FastenerCatalog.MATERIALS.containsKey(rawMat)) {
            rawMat
        } else {
            FastenerCatalog.MATERIALS.keys.find { rawMat.isNotBlank() && rawMat.contains(it) }
                ?: ShopDefaults.material.key.also { assumed += if (rawMat.isBlank()) "material (not on the drawing: $it used)" else "material ('$rawMat' is not in the catalogue: $it used)" }
        }

        val rawPropText = parsedJson.optString("propertyClass", "")
        // A tensile band in the notes replaces a property class
        val notesText = (listOf(rawPropText, parsedJson.optString("summary", "")) +
            (parsedJson.optJSONArray("detectedDimensions")?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                .map { "${it.optString("feature")} ${it.optString("nominal")} ${it.optString("tolerance")}" } } ?: emptyList()))
        val tensileRange = TensileRange.of(shown(parsedJson, "tensileMinMpa"), shown(parsedJson, "tensileMaxMpa"))
            ?: notesText.filter { "TENSILE" in it.uppercase() || "MPA" in it.uppercase() || "KSI" in it.uppercase() }.firstNotNullOfOrNull { TensileRange.parse(it) }
        val threadLockPatch = parsedJson.optBoolean("threadLockPatch", false) ||
            notesText.any { val u = it.uppercase(); "PATCH" in u || "PELLET" in u || "LOCKING ELEMENT" in u }
        if (rawPropText.isBlank() && tensileRange == null && !fastenerType.isWasher) assumed += "property class (not on the drawing: ${ShopDefaults.propertyClass.label} used)"
        val rawProp = rawPropText.uppercase()
        val matchedPropClass = when {
            rawProp.isBlank() -> ShopDefaults.propertyClass
            // Longest / most specific patterns first: "9.8" must not fall into "8.8", "A4-80" not into "A2-70"
            rawProp.contains("B7") -> PropertyClass.ASTM_A193_B7
            rawProp.contains("A574") -> PropertyClass.ASTM_A574
            rawProp.contains("GRADE 8") || rawProp.contains("GR 8") || rawProp.contains("GR.8") -> PropertyClass.SAE_GRADE_8
            rawProp.contains("GRADE 5") || rawProp.contains("GR 5") || rawProp.contains("GR.5") -> PropertyClass.SAE_GRADE_5
            rawProp.contains("GRADE 2") || rawProp.contains("GR 2") || rawProp.contains("GR.2") -> PropertyClass.SAE_GRADE_2
            rawProp.contains("12.9") -> PropertyClass.CLASS_12_9
            rawProp.contains("10.9") -> PropertyClass.CLASS_10_9
            rawProp.contains("9.8") -> PropertyClass.CLASS_9_8
            rawProp.contains("8.8") -> PropertyClass.CLASS_8_8
            rawProp.contains("6.8") -> PropertyClass.CLASS_6_8
            rawProp.contains("5.8") -> PropertyClass.CLASS_5_8
            rawProp.contains("5.6") -> PropertyClass.CLASS_5_6
            rawProp.contains("4.8") -> PropertyClass.CLASS_4_8
            rawProp.contains("4.6") -> PropertyClass.CLASS_4_6
            (rawProp.contains("A2") || rawProp.contains("A4")) && rawProp.contains("80") -> PropertyClass.A4_80
            (rawProp.contains("A2") || rawProp.contains("A4")) && rawProp.contains("50") -> PropertyClass.A2_50
            rawProp.contains("A2") || rawProp.contains("A4") || rawProp.contains("70") -> PropertyClass.A2_70
            rawProp.contains("1100") || rawProp.contains("TI") -> PropertyClass.AERO_1100
            rawProp.contains("1380") || rawProp.contains("INCONEL") || rawProp.contains("WASP") -> PropertyClass.AERO_1380
            tensileRange != null -> PropertyClass.MATERIAL_SPEC
            else -> ShopDefaults.propertyClass.also { assumed += "property class ('$rawPropText' not recognised: ${it.label} used)" }
        }

        // No class on the drawing: the governing standard's own class for this part, not 6g on everything
        val rawThreadClass = parsedJson.optString("threadClass", "").ifBlank {
            SelectionFilter.threadClasses(unitSystem, fastenerType, PokaYokeEngine.defaultStandard(fastenerType, unitSystem)).first()
        }
        val rawCoating = normEnum(parsedJson.optString("coating", ""))
        // Cadmium is specified by QQ-P-416 class; read the class from the note when the model gave free text
        val cadText = (listOf(parsedJson.optString("coating", "")) + notesText).joinToString(" ").uppercase()
        val cadmiumByClass = if (CoatingType.values().none { it.name == rawCoating } && ("CADMIUM" in cadText || "QQ-P-416" in cadText)) when {
            Regex("""CLASS\s*2\b""").containsMatchIn(cadText) -> CoatingType.AERO_CAD_CL2
            Regex("""CLASS\s*3\b""").containsMatchIn(cadText) -> CoatingType.AERO_CAD_CL3
            else -> CoatingType.AERO_CAD
        } else null
        val matchedCoating = cadmiumByClass ?: (if (rawCoating.isBlank()) null else CoatingType.values().find { it.name == rawCoating }
            ?: CoatingType.values().find { normEnum(it.label).contains(rawCoating) || rawCoating.contains(it.name) })
            ?: ShopDefaults.coating.also { assumed += "coating (not on the drawing: ${it.label} used)" }
        val summary = parsedJson.optString("summary", "Successfully extracted blueprint features.")

        val dimensionsList = mutableListOf<DetectedDimension>()
        val dimsArray = parsedJson.optJSONArray("detectedDimensions")
        if (dimsArray != null) {
            for (i in 0 until dimsArray.length()) {
                val dObj = dimsArray.optJSONObject(i)
                if (dObj != null) {
                    val feature = dObj.optString("feature", "").ifBlank { dObj.optString("name", "Dimension") }
                    val nominal = dObj.optString("nominal", "").ifBlank { dObj.optString("value", "") }
                    val tolerance = dObj.optString("tolerance", "").ifBlank { dObj.optString("limit", "") }
                    if (nominal.isNotBlank() || feature.isNotBlank()) {
                        dimensionsList.add(DetectedDimension(feature, nominal, tolerance))
                    }
                }
            }
        }
        if (dimensionsList.isEmpty()) {
            dimensionsList.add(DetectedDimension("Body Diameter", "${extractedDia} mm", "NOM"))
            dimensionsList.add(DetectedDimension("Thread Pitch", "${extractedPitch} mm", "REF"))
            dimensionsList.add(DetectedDimension("Nominal Length", "${extractedLength} mm", "NOM"))
        }

        val extractedRadius = parsedJson.optDouble("radius", Double.NaN).let { if (it.isNaN()) null else it }

        fun geom(key: String) = shown(parsedJson, key)
        val (solids, cutouts) = parseSolids(parsedJson)
        return ExtractedFastenerSpec(
            headDiaMm = geom("headDiaMm"),
            socketSizeMm = geom("socketSizeMm"),
            socketDepthMm = geom("socketDepthMm"),
            neckHeightMm = geom("neckHeightMm"),
            nutHeightMm = geom("nutHeightMm"),
            washerOuterDiaMm = geom("washerOuterDiaMm"),
            washerInnerDiaMm = geom("washerInnerDiaMm"),
            washerThicknessMm = geom("washerThicknessMm"),
            headDomeRadiusMm = geom("headDomeRadiusMm"),
            headEdgeHeightMm = geom("headEdgeHeightMm"),
            tensileMinMpa = tensileRange?.minMpa,
            tensileMaxMpa = tensileRange?.maxMpa,
            threadLockPatch = threadLockPatch,
            batchQty = shown(parsedJson, "quantity")?.toInt()?.takeIf { it > 0 },
            assumed = assumed,
            solids = solids,
            cutouts = cutouts,
            threadLengthMm = geom("threadLengthMm"),
            headAcrossFlatsMm = geom("headAcrossFlatsMm"),
            headHeightMm = geom("headHeightMm"),
            flangeDiaMm = geom("flangeDiaMm"),
            flangeThicknessMm = geom("flangeThicknessMm"),
            shankDiaMm = geom("shankDiaMm"),
            fastenerType = fastenerType,
            unitSystem = unitSystem,
            nominalSizeIndex = sizeIndex,
            nominalDiaMm = extractedDia,
            pitchValue = extractedPitch,
            lengthMm = extractedLength,
            propertyClass = matchedPropClass,
            materialKey = matchedMaterialKey,
            threadClass = rawThreadClass,
            coating = matchedCoating,
            technicalSummary = summary,
            detectedDimensions = dimensionsList,
            isAiVisionExtracted = true,
            modelUsed = modelName,
            radius = extractedRadius
        )
    }

    /** The scanned drawing as text: what was read, every dimension, and the total volume with its breakdown. */
    fun drawingContext(spec: ExtractedFastenerSpec, result: FastenerCalculationResult): String = buildString {
        fun n(v: Double, dp: Int = 2) = String.format(java.util.Locale.US, "%.${dp}f", v)
        val vol = DrawingScan.volume(spec, result)
        append("${spec.fastenerType.label}, ${result.threadLabel}, L ${n(spec.lengthMm)} mm, material ${result.material.name}, class ${spec.propertyClass.label.substringBefore(" (")}, coating ${spec.coating.label}.\n")
        append("Summary: ${spec.technicalSummary}\n")
        append("TOTAL VOLUME AS PER DRAWING: ${n(vol.totalMm3, 0)} mm³ = ${n(vol.massG)} g; cut-off slug Ø${n(vol.wireDiaMm)} × ${n(vol.cutOffLengthMm)} mm")
        append("\nHEAD VOLUME: ${n(vol.headMm3, 0)} mm³ = ${n(vol.headMassG)} g; SHANK + THREAD: ${n(vol.shankMm3, 0)} mm³ (head + shank = total)")
        append(if (vol.isComplete) " (complete: every dimension from the drawing).\n" else " (NOT complete, see below).\n")
        append("Volume basis: ${vol.basis.label}.\n")
        vol.items.forEach { append("- ${it.feature}: ${it.detail} = ${n(it.volumeMm3, 0)} mm³ [${it.source.label}]\n") }
        if (vol.notOnDrawing.isNotEmpty()) append("Not on the drawing, value used instead: ${vol.notOnDrawing.joinToString("; ")}.\n")
        if (vol.cautions.isNotEmpty()) append("Not in the volume / to check: ${vol.cautions.joinToString(" ")}\n")
        if (spec.assumed.isNotEmpty()) append("Defaults used because the drawing did not show them: ${spec.assumed.joinToString("; ")}.\n")
        if (spec.detectedDimensions.isNotEmpty()) {
            append("All dimensions read from the drawing:\n")
            spec.detectedDimensions.take(60).forEach { append("- ${it.feature}: ${it.nominal} ${it.tolerance}".trimEnd() + "\n") }
        }
    }

    suspend fun askCoPilot(
        prompt: String,
        currentResult: FastenerCalculationResult?,
        extractedSpec: ExtractedFastenerSpec?,
        imageBitmap: Bitmap?,
        context: Context,
        history: List<ChatMessageData> = emptyList(),
        /** The Sizing selection, secondary to a scanned drawing (currentResult is then the drawing's calculation). */
        sizingResult: FastenerCalculationResult? = null,
        /** Language to answer in; null = the app language. */
        replyLanguage: com.aistudio.aerofast.trmst.util.AppLanguage? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()

        val apiKey = getEffectiveApiKey(context)
        val aiConfig = AiSettings.load(context)
        val hasLiveKey = AiSettings.isConfigured(context)

        // Offline first: the app's own calculations, tables, rules and approved facts answer before any AI is asked
        val answerLang = replyLanguage ?: try { com.aistudio.aerofast.trmst.util.AppLocalization.getSelectedLanguage(context) } catch (_: Exception) { com.aistudio.aerofast.trmst.util.AppLanguage.EN }
        val knowledge = OfflineKnowledge.answer(trimmedPrompt, currentResult, answerLang, ApprovedFacts.load(context))
        val asksWhy = Regex("""\b(why|explain|how come|difference|reason)\b|ఎందుకు|వివరించ|क्यों|समझा|por qué|warum|pourquoi|почему|为什么|なぜ""", RegexOption.IGNORE_CASE).containsMatchIn(trimmedPrompt)
        if (imageBitmap == null) {
            if (knowledge.found && !(asksWhy && hasLiveKey)) return@withContext knowledge.withSources(answerLang)
            if (!hasLiveKey) return@withContext knowledge.withSources(answerLang)
        } else if (!hasLiveKey) {
            return@withContext "📷 Offline: reading an attached image needs an AI provider (🔑). Use the AI Scanner tab — it reads the drawing's numbers on the phone without internet."
        }

        // Cloud Gemini Prompt with Complete Materials Catalog and Engineering Instructions
        val promptText = buildString {
            append("AeroFast AI Co-Pilot expert in aerospace fastener engineering, metallurgy, cold/warm heading, and CNC tooling.\n")
            if (extractedSpec != null && currentResult != null) {
                append("PRIMARY INPUT — SCANNED DRAWING. Answer from this first; every calculation below is for this drawn part:\n")
                append(drawingContext(extractedSpec, currentResult))
                if (sizingResult != null) append("SECONDARY INPUT — Sizing tab selection (use only if the user asks about it): ${sizingResult.fastenerType.label} ${sizingResult.threadLabel}, ${sizingResult.material.name}.\n")
            } else {
                append("No drawing has been scanned. Input is the Sizing tab selection:\n")
            }
            append("Active Setup: ${currentResult?.fastenerType?.label ?: "Fastener"} ${currentResult?.threadLabel ?: ""}, Material: ${currentResult?.material?.name ?: ""} (UTS: ${currentResult?.material?.uts ?: 0} MPa).\n")
            if (currentResult != null) {
                val ht = currentResult.heatTreatmentSpec
                append("Dynamic Heat Treatment & Soaking Schedule (AMS 2759 / ISO 898-1 for Ø${currentResult.nominalDiaMm}mm):\n")
                append("- Stage 1 Pre-Heat: ${ht.preheatTempC.ifBlank { "per ${ht.standardReference}" }} (${ht.preheatSoakTime.ifBlank { "per ${ht.standardReference}" }})\n")
                append("- Stage 2 ${if (ht.isAging) "Solution" else "Austenitize"}: ${ht.austenitizingOrSolutionTempC} (${ht.soakingTimeMinutes})\n")
                append("- Stage 3 Quench: ${ht.quenchMedium} (${ht.quenchDetails.ifBlank { "per ${ht.standardReference}" }})\n")
                append("- Stage 4 ${if (ht.isAging) "Aging" else "Temper"}: ${ht.temperingOrAgingTempC} (${ht.temperingOrAgingTimeHours})\n")
                if (currentResult.hasHydrogenEmbrittlementRisk) {
                    append("- Stage 5 Mandatory Hydrogen Bake: ${ht.bakingTempC} (${ht.bakingSoakTime})\n")
                }
            }
            if (currentResult != null) append(forgingContext(currentResult))

            // The 50+ alloy catalog costs ~2,000 tokens, so only send it when the question is about materials
            val qMat = trimmedPrompt.lowercase()
            val asksAboutMaterials = listOf("material", "alloy", "steel", "catalog", "grade", "inconel", "titanium", "stainless", "nickel", "list")
                .any { qMat.contains(it) } ||
                FastenerCatalog.MATERIALS.keys.any { it.length >= 3 && qMat.contains(it.lowercase()) }
            if (asksAboutMaterials) {
                append("\nAPP MATERIAL CATALOG [key] name | UTS MPa | density | km | HT std:\n")
                FastenerCatalog.MATERIALS.values.forEach { m ->
                    append("[${m.key}] ${m.name} | ${m.uts.toInt()} | ${m.density} | ${m.km} | ${m.heatTreatmentSpec.standardReference}\n")
                }
                append("For alloys asked about: say which are in the catalog; for missing ones give key, UTS, density, km, cutting speed, HT standard, and note they can be added with '+ Add Material' on the Sizing screen.\n")
            }

            append("\nACCURACY: Use only the app data above and facts you are certain of from published standards. Never guess or invent a number, limit, clause or standard. ")
            append("If a value is not in the data and you are not certain, say so plainly and name the document to check (the drawing, the standard, the supplier's certificate). ")
            append("\nRULES: Answer the actual question first, completely: every key fact, number, limit and standard the user needs, in short bullets. ")
            append("No introduction, no repetition, no closing offers or questions. Answer general engineering questions even if unrelated to the setup. ")
            append("Plain text only: no LaTeX, no dollar-sign math, no tables; write units inline (Rm >= 1040 MPa, 200 °C, Ø8.5 mm).\n")

            // Recent conversation, trimmed, so follow-up questions ("why?", "and for M12?") keep their context
            val recent = history.filter { it.text.isNotBlank() }.takeLast(6)
            if (recent.isNotEmpty()) {
                append("\nRECENT CONVERSATION (oldest first):\n")
                recent.forEach { m ->
                    append(if (m.isUser) "User: " else "Co-Pilot: ")
                    append(m.text.take(if (m.isUser) 300 else 500).replace("\n", " "))
                    append("\n")
                }
            }

            val currentLang = replyLanguage ?: try { com.aistudio.aerofast.trmst.util.AppLocalization.getSelectedLanguage(context) } catch (_: Exception) { com.aistudio.aerofast.trmst.util.AppLanguage.EN }
            if (currentLang != com.aistudio.aerofast.trmst.util.AppLanguage.EN) {
                append("\nCRITICAL USER LANGUAGE REQUIREMENT:\n")
                append("The user has configured the application language to: ${currentLang.displayName} (${currentLang.code}).\n")
                append("You MUST respond in ${currentLang.displayName} (${currentLang.code})! Translate all your explanations, technical recommendations, process flows, and metallurgy guidance naturally into ${currentLang.displayName}, while keeping standard fastener specifications (e.g., M10, 6g, ISO 898-1, AMS 2759, 10.9, HRC, MPa, G76, ASTM B850) in clear international engineering notation.\n")
            }

            if (imageBitmap != null) {
                append("\n[NEW USER ATTACHED BLUEPRINT/PHOTO]: Inspect image and answer: \"$trimmedPrompt\"\n")
            } else {
                append("\nUser Query: \"$trimmedPrompt\"\n")
            }
        }

        if (aiConfig.provider != AiProvider.GEMINI) {
            val answer = OpenAiCompatClient.chat(aiConfig, promptText, imageBitmap?.let { bitmapToBase64(it) })
            answer.getOrNull()?.let { return@withContext it.trim() }
            val why = answer.exceptionOrNull()?.message ?: "no response"
            val offlineNote = "\n\n_⚠️ Offline answer — ${aiConfig.provider.label} unavailable ($why)._"
            return@withContext if (knowledge.found) knowledge.withSources(answerLang) + offlineNote
                else "⚠️ ${aiConfig.provider.label} did not answer: $why\n\n" + knowledge.withSources(answerLang)
        }

        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", promptText))
                // Only include inlineData if a brand-new image was attached in this message
                if (imageBitmap != null) {
                    val base64 = bitmapToBase64(imageBitmap)
                    partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    }))
                }
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                // Gemini 3 models count "thinking" against this budget; 1536 cut answers off after a line or two
                put("maxOutputTokens", 8192)
                put("thinkingConfig", JSONObject().put("thinkingLevel", "minimal"))
            })
        }

        var lastError: String? = null
        for (model in MODEL_FALLBACK_CHAIN) {
            try {
                checkRateLimit() // Enforce rate limiting
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                    .header("User-Agent", "AeroFastPro/1.0")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = httpClient.newCall(request).execute()
                val responseBody = response.use { it.body?.string() ?: "" }
                if (!response.isSuccessful) {
                    lastError = describeHttpError(model, response.code, responseBody)
                    continue
                }

                val rootJson = JSONObject(responseBody)

                // Validate response structure
                if (!rootJson.has("candidates")) {
                    lastError = "Invalid API response: missing 'candidates' field"
                    continue
                }

                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    lastError = "Model $model returned no candidates"
                    continue
                }

                val firstCandidate = candidates.optJSONObject(0) ?: run {
                    lastError = "Invalid candidate object"
                    continue
                }

                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                if (parts == null || parts.length() == 0) {
                    lastError = "Model $model returned no response parts"
                    continue
                }

                var fullText = ""
                for (p in 0 until parts.length()) {
                    val partObj = parts.optJSONObject(p) ?: continue
                    val isThought = partObj.optBoolean("thought", false)
                    if (!isThought) {
                        val pText = partObj.optString("text", "").trim()
                        if (pText.isNotBlank()) {
                            fullText += pText
                        }
                    }
                }

                if (fullText.isNotBlank()) {
                    return@withContext fullText.trim()
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage
            }
        }

        // Final fallback: return deterministic local calculation, flagged so the user knows the AI was not reached
        val offlineNote = "\n\n_⚠️ Offline answer — Gemini AI unavailable (${lastError ?: "no response"})._"
        return@withContext if (knowledge.found) knowledge.withSources(answerLang) + offlineNote
            else "⚠️ Gemini did not answer: ${lastError ?: "no response"}\n\n" + knowledge.withSources(answerLang)
    }
}
