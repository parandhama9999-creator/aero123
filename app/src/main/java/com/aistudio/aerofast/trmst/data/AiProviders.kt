package com.aistudio.aerofast.trmst.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Where the AI answers come from. Model names change often, so the suggested model is only a starting value:
 * "Load models" in Settings reads the provider's live list.
 */
enum class AiProvider(val label: String, val defaultBaseUrl: String, val suggestedModel: String, val note: String) {
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com", "",
        "Reads drawings, answers the Co-Pilot and Jarvis. Key from Google AI Studio."),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openrouter/free",
        "Models ending in “:free” cost nothing but are rate-limited; “openrouter/free” lets OpenRouter pick a free one. Key from openrouter.ai."),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-flash",
        "Paid per token. Text only (cannot read drawing images). Load models to see the current names."),
    CUSTOM("Custom (OpenAI-compatible)", "", "auto",
        "Any OpenAI-compatible /v1 endpoint, e.g. a FreeLLMAPI gateway you host (model “auto”). The address must start with https://.")
}

data class AiConfig(val provider: AiProvider, val baseUrl: String, val model: String, val apiKey: String)

data class AiModelInfo(val id: String, val free: Boolean)

/** The chosen provider, its address and model, and one key per provider, all in the encrypted store. */
object AiSettings {
    private const val PREFS = "aerofast_ai_provider_secure"

    fun load(context: Context): AiConfig = try {
        val p = GeminiAiService.encryptedPrefs(context, PREFS)
        val provider = runCatching { AiProvider.valueOf(p.getString("provider", null)!!) }.getOrDefault(AiProvider.GEMINI)
        AiConfig(
            provider = provider,
            baseUrl = p.getString("base_${provider.name}", null)?.takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl,
            model = p.getString("model_${provider.name}", null)?.takeIf { it.isNotBlank() } ?: provider.suggestedModel,
            apiKey = if (provider == AiProvider.GEMINI) GeminiAiService.getEffectiveApiKey(context) else p.getString("key_${provider.name}", "").orEmpty()
        )
    } catch (_: Exception) {
        AiConfig(AiProvider.GEMINI, AiProvider.GEMINI.defaultBaseUrl, "", GeminiAiService.getEffectiveApiKey(context))
    }

    /** Saved values for a provider without making it the active one (for the Settings form). */
    fun loadFor(context: Context, provider: AiProvider): AiConfig = try {
        val p = GeminiAiService.encryptedPrefs(context, PREFS)
        AiConfig(
            provider,
            p.getString("base_${provider.name}", null)?.takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl,
            p.getString("model_${provider.name}", null)?.takeIf { it.isNotBlank() } ?: provider.suggestedModel,
            if (provider == AiProvider.GEMINI) GeminiAiService.getEffectiveApiKey(context) else p.getString("key_${provider.name}", "").orEmpty()
        )
    } catch (_: Exception) {
        AiConfig(provider, provider.defaultBaseUrl, provider.suggestedModel, "")
    }

    fun save(context: Context, cfg: AiConfig) {
        if (cfg.provider == AiProvider.GEMINI) GeminiAiService.saveUserApiKey(context, cfg.apiKey)
        val e = GeminiAiService.encryptedPrefs(context, PREFS).edit()
            .putString("provider", cfg.provider.name)
            .putString("base_${cfg.provider.name}", cfg.baseUrl.trim())
            .putString("model_${cfg.provider.name}", cfg.model.trim())
        if (cfg.provider != AiProvider.GEMINI) e.putString("key_${cfg.provider.name}", cfg.apiKey.trim())
        e.apply()
    }

    /** True when the active provider can be called: a key (custom endpoints may run without one), an https address and a model. */
    fun isConfigured(context: Context): Boolean {
        val c = load(context)
        return if (c.provider == AiProvider.GEMINI) GeminiAiService.hasLiveApiKey(context) else isUsable(c)
    }

    fun isUsable(cfg: AiConfig): Boolean = when (cfg.provider) {
        AiProvider.GEMINI -> cfg.apiKey.isNotBlank()
        AiProvider.CUSTOM -> OpenAiCompatClient.baseUrlProblem(cfg.baseUrl) == null && cfg.model.isNotBlank()
        else -> cfg.apiKey.isNotBlank() && OpenAiCompatClient.baseUrlProblem(cfg.baseUrl) == null && cfg.model.isNotBlank()
    }

    /** Short label of what is answering, for the screens. */
    fun describe(context: Context): String = load(context).let { c ->
        if (c.provider == AiProvider.GEMINI) "Gemini" else "${c.provider.label}: ${c.model}"
    }
}

/** OpenAI-format chat completions (OpenRouter, DeepSeek, FreeLLMAPI and other compatible gateways). */
object OpenAiCompatClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Why an address cannot be used, or null. Plain http is refused: the app allows encrypted traffic only. */
    fun baseUrlProblem(url: String): String? {
        val u = url.trim()
        return when {
            u.isBlank() -> "Enter the provider's base address (ending in /v1 for most gateways)."
            !u.startsWith("https://") -> "The address must start with https:// — the app sends keys and drawings over encrypted connections only."
            u.length < 12 -> "The address looks incomplete."
            else -> null
        }
    }

    fun endpoint(baseUrl: String, path: String) = baseUrl.trim().trimEnd('/') + path

    /** One user message: text, plus a JPEG image for vision models. */
    fun buildChatBody(model: String, prompt: String, imageBase64Jpeg: String?, maxTokens: Int, temperature: Double): JSONObject {
        val content: Any = if (imageBase64Jpeg == null) prompt else JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64Jpeg")))
        return JSONObject()
            .put("model", model.trim())
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
            .put("max_tokens", maxTokens)
            .put("temperature", temperature)
    }

    /** The reply text, or the provider's own error message. */
    fun parseChatResponse(body: String): Result<String> = try {
        val o = JSONObject(body)
        o.optJSONObject("error")?.let { return Result.failure(IllegalStateException(it.optString("message", it.toString()))) }
        val msg = o.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
            ?: return Result.failure(IllegalStateException("The provider returned no answer."))
        val c = msg.opt("content")
        val text = when (c) {
            is String -> c
            is JSONArray -> (0 until c.length()).mapNotNull { c.optJSONObject(it)?.optString("text") }.joinToString("")
            else -> ""
        }.trim()
        if (text.isBlank()) Result.failure(IllegalStateException("The provider returned an empty answer.")) else Result.success(text)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Unreadable provider reply: ${e.message}"))
    }

    /** Model ids from GET /models; OpenRouter marks free ones with “:free” or zero prompt and completion prices. */
    fun parseModels(body: String): List<AiModelInfo> {
        val arr = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.mapNotNull { m ->
            val id = m.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val pr = m.optJSONObject("pricing")
            val zeroPrice = pr != null && pr.optString("prompt") .toDoubleOrNull() == 0.0 && pr.optString("completion").toDoubleOrNull() == 0.0
            AiModelInfo(id, id.endsWith(":free") || zeroPrice)
        }.sortedWith(compareBy({ !it.free }, { it.id }))
    }

    private fun request(cfg: AiConfig, path: String): Request.Builder = Request.Builder()
        .url(endpoint(cfg.baseUrl, path))
        .header("User-Agent", "AeroFastPro/1.0")
        .apply {
            if (cfg.apiKey.isNotBlank()) header("Authorization", "Bearer ${cfg.apiKey.trim()}")
            if (cfg.provider == AiProvider.OPENROUTER) header("X-Title", "AeroFast Pro")
        }

    suspend fun chat(cfg: AiConfig, prompt: String, imageBase64Jpeg: String? = null, maxTokens: Int = 4096, temperature: Double = 0.3): Result<String> =
        withContext(Dispatchers.IO) {
            baseUrlProblem(cfg.baseUrl)?.let { return@withContext Result.failure(IllegalStateException(it)) }
            if (cfg.model.isBlank()) return@withContext Result.failure(IllegalStateException("Choose a model in Settings."))
            try {
                val body = buildChatBody(cfg.model, prompt, imageBase64Jpeg, maxTokens, temperature).toString()
                    .toRequestBody("application/json".toMediaType())
                client.newCall(request(cfg, "/chat/completions").post(body).build()).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        val reason = parseChatResponse(text).exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: text.take(200)
                        Result.failure(IllegalStateException("${cfg.provider.label} (${cfg.model}) HTTP ${resp.code}: $reason"))
                    } else parseChatResponse(text).fold(
                        { Result.success(it) },
                        { Result.failure(IllegalStateException("${cfg.provider.label} (${cfg.model}): ${it.message}")) }
                    )
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException("${cfg.provider.label} not reached: ${e.message}"))
            }
        }

    suspend fun listModels(cfg: AiConfig): Result<List<AiModelInfo>> = withContext(Dispatchers.IO) {
        baseUrlProblem(cfg.baseUrl)?.let { return@withContext Result.failure(IllegalStateException(it)) }
        try {
            client.newCall(request(cfg, "/models").get().build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) Result.failure(IllegalStateException("HTTP ${resp.code}: ${text.take(200)}"))
                else Result.success(parseModels(text))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Model list not reached: ${e.message}"))
        }
    }
}
