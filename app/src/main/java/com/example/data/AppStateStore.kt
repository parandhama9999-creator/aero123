package com.example.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

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

/**
 * App state, kept on the phone.
 *
 * This used to mirror every save to a Firebase Realtime Database as well. That mirror could never
 * be read back: the record was addressed by a random UUID held in the same SharedPreferences file
 * as the data itself, so the two were always lost together. In the one case where a remote copy
 * would have earned its keep -- the local data being gone -- the id needed to find it was gone too,
 * and a fresh install simply minted a new UUID and a new empty record. Removing it costs nothing
 * and the app no longer needs a network, a backend or a billing account.
 */
object AppStateStore {
    private const val TAG = "AppStateStore"
    private const val PREFS_NAME = "aerofast_local_storage"
    private const val KEY_APP_DATA = "aerofast_app_data"

    fun save(context: Context, state: AeroFastState) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_APP_DATA, state.toJson().toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Could not save app state", e)
        }
    }

    fun load(context: Context): AeroFastState? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_DATA, null)
            ?.let { AeroFastState.fromJson(it) }
}
