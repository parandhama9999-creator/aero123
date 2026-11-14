package com.example.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatMessageData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AeroFastState(
    var fastenerType: String = FastenerType.HEX_BOLT.name,
    var unitSystem: String = UnitSystem.METRIC.name,
    var standard: String = ProductStandard.ISO_4014.name,
    var sizeIndex: Int = 11,
    /** Nominal diameter in mm; restores the size even when the size list changes. 0 = saved by an older version. */
    var sizeDiaMm: Double = 0.0,
    var pitchValue: Double = 1.50,
    var lengthMm: Double = 60.0,
    var propClass: String = PropertyClass.CLASS_10_9.name,
    var materialKey: String = "10B21",
    var threadClass: String = "6g",
    var coating: String = CoatingType.ZN_NI.name,
    var batchQty: Int = 10000,
    var currencyIsUsd: Boolean = false,
    var cncHourRate: Double = 750.0,
    var htRatePerKg: Double = 180.0,
    var marginPct: Double = 22.0,
    var fontOption: String = "ROBOTO_MONO",
    var currentTier: String = "FREE",
    var lastScannedBlueprint: String? = null,
    var chatMessages: List<ChatMessageData> = emptyList(),
    var lastUpdated: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("fastenerType", fastenerType)
        obj.put("unitSystem", unitSystem)
        obj.put("standard", standard)
        obj.put("sizeIndex", sizeIndex)
        obj.put("sizeDiaMm", sizeDiaMm)
        obj.put("pitchValue", pitchValue)
        obj.put("lengthMm", lengthMm)
        obj.put("propClass", propClass)
        obj.put("materialKey", materialKey)
        obj.put("threadClass", threadClass)
        obj.put("coating", coating)
        obj.put("batchQty", batchQty)
        obj.put("currencyIsUsd", currencyIsUsd)
        obj.put("cncHourRate", cncHourRate)
        obj.put("htRatePerKg", htRatePerKg)
        obj.put("marginPct", marginPct)
        obj.put("fontOption", fontOption)
        obj.put("currentTier", currentTier)
        obj.put("lastScannedBlueprint", lastScannedBlueprint ?: "")
        obj.put("lastUpdated", System.currentTimeMillis())

        val msgArray = JSONArray()
        for (msg in chatMessages) {
            val msgObj = JSONObject()
            msgObj.put("id", msg.id)
            msgObj.put("isUser", msg.isUser)
            msgObj.put("text", msg.text)
            msgObj.put("timestamp", msg.timestamp)
            msgArray.put(msgObj)
        }
        obj.put("chatMessages", msgArray)

        return obj
    }

    /**
     * The shape written to Firebase. Types here must match the .validate rules in
     * database.rules.json exactly — a Long where the rule expects a number is fine, a null is not,
     * and any extra key is rejected outright by the "$other": false guard.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "fastenerType" to fastenerType,
        "unitSystem" to unitSystem,
        "standard" to standard,
        "sizeIndex" to sizeIndex,
        "sizeDiaMm" to sizeDiaMm,
        "pitchValue" to pitchValue,
        "lengthMm" to lengthMm,
        "propClass" to propClass,
        "materialKey" to materialKey,
        "threadClass" to threadClass,
        "coating" to coating,
        "batchQty" to batchQty,
        "currencyIsUsd" to currencyIsUsd,
        "cncHourRate" to cncHourRate,
        "htRatePerKg" to htRatePerKg,
        "marginPct" to marginPct,
        "fontOption" to fontOption,
        "currentTier" to currentTier,
        "lastScannedBlueprint" to (lastScannedBlueprint ?: ""),
        "lastUpdated" to System.currentTimeMillis(),
        "chatMessages" to chatMessages.map {
            mapOf("id" to it.id, "isUser" to it.isUser, "text" to it.text, "timestamp" to it.timestamp)
        }
    )

    companion object {
        fun fromJson(jsonStr: String): AeroFastState? {
            return try {
                val obj = JSONObject(jsonStr)
                val chatList = mutableListOf<ChatMessageData>()
                if (obj.has("chatMessages")) {
                    val arr = obj.getJSONArray("chatMessages")
                    for (i in 0 until arr.length()) {
                        val mObj = arr.getJSONObject(i)
                        chatList.add(
                            ChatMessageData(
                                id = mObj.optString("id", java.util.UUID.randomUUID().toString()),
                                isUser = mObj.optBoolean("isUser", false),
                                text = mObj.optString("text", ""),
                                timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }
                AeroFastState(
                    fastenerType = obj.optString("fastenerType", FastenerType.HEX_BOLT.name),
                    unitSystem = obj.optString("unitSystem", UnitSystem.METRIC.name),
                    standard = obj.optString("standard", ProductStandard.ISO_4014.name),
                    sizeIndex = obj.optInt("sizeIndex", 11),
                    sizeDiaMm = obj.optDouble("sizeDiaMm", 0.0),
                    pitchValue = obj.optDouble("pitchValue", 1.50),
                    lengthMm = obj.optDouble("lengthMm", 60.0),
                    propClass = obj.optString("propClass", PropertyClass.CLASS_10_9.name),
                    materialKey = obj.optString("materialKey", "10B21"),
                    threadClass = obj.optString("threadClass", "6g"),
                    coating = obj.optString("coating", CoatingType.ZN_NI.name),
                    batchQty = obj.optInt("batchQty", 10000),
                    currencyIsUsd = obj.optBoolean("currencyIsUsd", false),
                    cncHourRate = obj.optDouble("cncHourRate", 750.0),
                    htRatePerKg = obj.optDouble("htRatePerKg", 180.0),
                    marginPct = obj.optDouble("marginPct", 22.0),
                    fontOption = obj.optString("fontOption", "ROBOTO_MONO"),
                    currentTier = obj.optString("currentTier", "FREE"),
                    lastScannedBlueprint = if (obj.has("lastScannedBlueprint") && !obj.isNull("lastScannedBlueprint")) obj.getString("lastScannedBlueprint") else null,
                    chatMessages = chatList,
                    lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                )
            } catch (e: Exception) {
                Log.e("AeroFastState", "Error parsing JSON", e)
                null
            }
        }
    }
}

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"
    private const val PREFS_NAME = "aerofast_local_storage"
    private const val KEY_APP_DATA = "aerofast_app_data"
    private const val NETWORK_TIMEOUT_MS = 2500L

    /**
     * The node for the signed-in licence, or null when the app is not activated.
     *
     * The key is the Firebase uid rather than a self-chosen device id, because the security rules
     * compare it against auth.uid: a client cannot name a node it does not own. Going through the
     * SDK rather than REST is what attaches the auth and App Check tokens to every request.
     */
    private fun deviceRef(): DatabaseReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseDatabase.getInstance().getReference("aerofast_devices").child(uid)
    }

    suspend fun syncAppData(context: Context, state: AeroFastState) {
        val jsonString = state.toJson().toString()
        saveToLocalStorage(context, jsonString)

        val ref = deviceRef() ?: return // not activated: local only
        try {
            withTimeout(NETWORK_TIMEOUT_MS) { ref.setValue(state.toMap()).awaitUnit() }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Sync timed out; local copy is saved.")
        } catch (e: Exception) {
            Log.w(TAG, "Sync to Firebase failed: ${e.message}")
        }
    }

    suspend fun fetchAppDataOnStartup(context: Context): AeroFastState? {
        val ref = deviceRef()
        if (ref != null) {
            val remote = try {
                withTimeout(NETWORK_TIMEOUT_MS) { ref.get().awaitResult() }
            } catch (e: Exception) {
                Log.w(TAG, "Startup fetch failed or timed out, using local storage: ${e.message}")
                null
            }
            val value = remote?.value
            if (value != null) {
                val json = JSONObject(value as? Map<*, *> ?: emptyMap<String, Any?>()).toString()
                AeroFastState.fromJson(json)?.let {
                    saveToLocalStorage(context, json)
                    return it
                }
            }
        }
        return getFromLocalStorage(context)?.let { AeroFastState.fromJson(it) }
    }

    private fun saveToLocalStorage(context: Context, jsonStr: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_APP_DATA, jsonStr).apply()
    }

    private fun getFromLocalStorage(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_APP_DATA, null)
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

private suspend fun Task<Void>.awaitUnit() { awaitResult() }
