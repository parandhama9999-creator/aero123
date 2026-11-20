package com.aistudio.aerofast.trmst.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MediaModel(val id: String, val displayName: String)
data class MediaModels(val image: List<MediaModel>, val video: List<MediaModel>)
data class AiImage(val bytes: ByteArray, val mimeType: String, val note: String)

/** State of a video job on the server. */
sealed class VideoJob {
    data class Running(val operation: String) : VideoJob()
    data class Ready(val uri: String?, val bytes: ByteArray?) : VideoJob()
    data class Failed(val message: String) : VideoJob()
}

/**
 * AI images (Gemini image models, generateContent with an IMAGE response) and AI video (Veo, predictLongRunning),
 * through the user's own Gemini key. Model names change, so they are read live from the key's model list rather
 * than written into the app. The results are illustrations: not drawn from the app's geometry, not to scale.
 */
object MediaAi {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS)
        .followRedirects(true).followSslRedirects(true)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun req(key: String, path: String) = Request.Builder().url(if (path.startsWith("http")) path else "$BASE/$path").header("x-goog-api-key", key)

    private fun error(code: Int, body: String): String {
        val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return "HTTP $code: ${msg?.takeIf { it.isNotBlank() } ?: body.take(200)}"
    }

    /** Image models: names with "image" (or nano-banana) that take generateContent. Video: "veo" with predictLongRunning. */
    fun classify(json: String): MediaModels {
        val arr = JSONObject(json).optJSONArray("models") ?: JSONArray()
        val image = mutableListOf<MediaModel>(); val video = mutableListOf<MediaModel>()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val id = m.optString("name").removePrefix("models/")
            val methods = m.optJSONArray("supportedGenerationMethods")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
            val label = m.optString("displayName").ifBlank { id }
            val lower = id.lowercase()
            if ((lower.contains("image") || lower.contains("nano-banana")) && !lower.startsWith("imagen") && "generateContent" in methods) image += MediaModel(id, label)
            if (lower.contains("veo") && "predictLongRunning" in methods) video += MediaModel(id, label)
        }
        return MediaModels(image.sortedByDescending { it.id }, video.sortedByDescending { it.id })
    }

    suspend fun listModels(key: String): Result<MediaModels> = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(req(key, "models?pageSize=1000").get().build()).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw IllegalStateException(error(r.code, body))
                classify(body)
            }
        }
    }

    /** Pulls the first image part (inlineData or inline_data) and any text out of a generateContent response. */
    fun parseImage(json: String): AiImage? {
        val parts = JSONObject(json).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return null
        var img: AiImage? = null
        val text = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            val inline = p.optJSONObject("inlineData") ?: p.optJSONObject("inline_data")
            if (inline != null && img == null) {
                val data = inline.optString("data")
                if (data.isNotEmpty()) img = AiImage(Base64.decode(data, Base64.DEFAULT), inline.optString("mimeType", inline.optString("mime_type", "image/png")), "")
            }
            p.optString("text").takeIf { it.isNotBlank() }?.let { text.append(it).append(' ') }
        }
        return img?.copy(note = text.toString().trim())
    }

    suspend fun generateImage(key: String, model: String, prompt: String): Result<AiImage> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("TEXT").put("IMAGE")))
            client.newCall(req(key, "models/$model:generateContent").post(body.toString().toRequestBody(JSON)).build()).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw IllegalStateException(error(r.code, text))
                parseImage(text) ?: throw IllegalStateException("The model answered without an image" +
                    (runCatching { JSONObject(text).optJSONObject("promptFeedback")?.toString() }.getOrNull()?.let { ": $it" } ?: "."))
            }
        }
    }

    suspend fun startVideo(key: String, model: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("instances", JSONArray().put(JSONObject().put("prompt", prompt)))
            client.newCall(req(key, "models/$model:predictLongRunning").post(body.toString().toRequestBody(JSON)).build()).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw IllegalStateException(error(r.code, text))
                JSONObject(text).optString("name").takeIf { it.isNotBlank() } ?: throw IllegalStateException("No operation name in the reply")
            }
        }
    }

    /** Finds the generated video in a finished operation: a "uri" under a video object, or inline base64 bytes. */
    fun parseOperation(json: String): VideoJob {
        val o = JSONObject(json)
        o.optJSONObject("error")?.let { return VideoJob.Failed(it.optString("message", it.toString())) }
        if (!o.optBoolean("done", false)) return VideoJob.Running(o.optString("name"))
        fun find(any: Any?, inVideo: Boolean): Pair<String?, String?>? {
            when (any) {
                is JSONObject -> {
                    if (inVideo) {
                        any.optString("uri").takeIf { it.isNotBlank() }?.let { return it to null }
                        any.optString("bytesBase64Encoded").takeIf { it.isNotBlank() }?.let { return null to it }
                    }
                    for (k in any.keys()) find(any.opt(k), inVideo || k.lowercase().contains("video"))?.let { return it }
                }
                is JSONArray -> for (i in 0 until any.length()) find(any.opt(i), inVideo)?.let { return it }
            }
            return null
        }
        val hit = find(o.opt("response"), false) ?: return VideoJob.Failed("The job finished without a video" +
            (o.optJSONObject("response")?.toString()?.take(300)?.let { ": $it" } ?: "."))
        return VideoJob.Ready(hit.first, hit.second?.let { Base64.decode(it, Base64.DEFAULT) })
    }

    suspend fun pollVideo(key: String, operation: String): Result<VideoJob> = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(req(key, operation).get().build()).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw IllegalStateException(error(r.code, text))
                parseOperation(text)
            }
        }
    }

    suspend fun download(key: String, uri: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(req(key, uri).get().build()).execute().use { r ->
                if (!r.isSuccessful) throw IllegalStateException(error(r.code, r.body?.string().orEmpty()))
                r.body?.bytes() ?: throw IllegalStateException("Empty download")
            }
        }
    }

    /** Prompt built from the current part; the user can edit it before sending. */
    fun imagePrompt(r: FastenerCalculationResult): String =
        "Studio product photograph of a single ${r.fastenerType.label.substringBefore(" (").substringBefore(" - ")}, " +
            "${r.threadLabel} thread, ${"%.0f".format(java.util.Locale.US, r.lengthMm)} mm long, ${r.material.name} steel, ${r.coating.label} finish, " +
            "lying on a neutral grey background, soft light, sharp focus, realistic industrial fastener"

    fun videoPrompt(r: FastenerCalculationResult): String =
        "Short realistic industrial video: a multi-station cold heading machine forms steel wire into a " +
            "${r.fastenerType.label.substringBefore(" (").substringBefore(" - ")} (${r.threadLabel}), close-up of the dies and the finished part dropping out, factory lighting"
}
