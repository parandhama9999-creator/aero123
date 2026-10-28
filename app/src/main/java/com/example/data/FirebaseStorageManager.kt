package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val FIREBASE_DB_URL = "https://aerofast-pro-default-rtdb.asia-southeast1.firebasedatabase.app"

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
    private const val KEY_DEVICE_ID = "aerofast_device_id"

    // Random per-install ID so each device syncs to its own record instead of one shared node
    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    private fun deviceDataUrl(context: Context): URL =
        URL("$FIREBASE_DB_URL/aerofast_devices/${getDeviceId(context)}.json")

    suspend fun syncAppData(context: Context, state: AeroFastState) {
        val jsonString = state.toJson().toString()
        saveToLocalStorage(context, jsonString)

        withContext(Dispatchers.IO) {
            try {
                val url = deviceDataUrl(context)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Firebase REST sync PUT response code: $responseCode")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Background sync to Firebase RTDB failed: ${e.message}")
            }
        }
    }

    suspend fun fetchAppDataOnStartup(context: Context): AeroFastState? {
        val remoteJsonStr = withContext(Dispatchers.IO) {
            try {
                val url = deviceDataUrl(context)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2000
                    readTimeout = 2000
                }

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()
                    connection.disconnect()
                    val result = sb.toString().trim()
                    if (result != "null" && result.isNotBlank()) result else null
                } else {
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network fetch from Firebase RTDB timed out (2s limit) or failed, falling back to local storage: ${e.message}")
                null
            }
        }

        if (!remoteJsonStr.isNullOrBlank()) {
            val remoteState = AeroFastState.fromJson(remoteJsonStr)
            if (remoteState != null) {
                saveToLocalStorage(context, remoteJsonStr)
                return remoteState
            }
        }

        val localJsonStr = getFromLocalStorage(context)
        return localJsonStr?.let { AeroFastState.fromJson(it) }
    }

    private fun saveToLocalStorage(context: Context, jsonStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_DATA, jsonStr).apply()
    }

    private fun getFromLocalStorage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_DATA, null)
    }
}
