package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The calculation engine is the Rust library libaerofast_engine.so (engine-rs/). Every engine function the app
 * calls is sent here by name with its arguments as JSON, and the answer comes back as JSON in the same shape as
 * the Kotlin types. A failure inside the engine comes back as [EngineException], never as a crash.
 */
class EngineException(message: String) : RuntimeException(message)

object NativeEngine {
    init {
        System.loadLibrary("aerofast_engine")
    }

    @JvmStatic
    external fun call(method: String, args: String): String

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = true
        allowSpecialFloatingPointValues = true
    }

    inline fun <reified T> a(value: T): JsonElement = json.encodeToJsonElement(value)

    inline fun <reified T> run(method: String, vararg args: JsonElement): T {
        val reply = json.parseToJsonElement(call(method, JsonArray(args.toList()).toString())).jsonObject
        reply["err"]?.let { throw EngineException(it.jsonPrimitive.content) }
        return json.decodeFromJsonElement(reply.getValue("ok"))
    }
}
