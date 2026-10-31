package com.example.server

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.UnitSystem
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.Executors

private val mapper = jacksonObjectMapper()
private val apiKey: String? = System.getenv("AEROFAST_API_KEY")?.takeIf { it.isNotBlank() }

class BadRequest(message: String) : Exception(message)

private fun JsonNode.text(name: String): String =
    get(name)?.asText()?.takeIf { it.isNotBlank() } ?: throw BadRequest("missing '$name'")

private fun JsonNode.num(name: String): Double =
    get(name)?.takeIf { it.isNumber }?.asDouble() ?: throw BadRequest("missing number '$name'")

private inline fun <reified E : Enum<E>> JsonNode.enumOf(name: String): E =
    runCatching { enumValueOf<E>(text(name)) }.getOrElse { throw BadRequest("unknown $name '${get(name)?.asText()}'") }

/** Runs exactly the calculation the app runs and returns the full result. */
fun calculate(body: JsonNode): Any {
    val materialKey = body.text("materialKey")
    val material = FastenerCatalog.MATERIALS[materialKey] ?: throw BadRequest("unknown materialKey '$materialKey'")
    return CalculationEngine.calculate(
        fastenerType = body.enumOf<FastenerType>("fastenerType"),
        unitSystem = body.enumOf<UnitSystem>("unitSystem"),
        standard = body.enumOf<ProductStandard>("standard"),
        nominalSizeIndex = body.num("nominalSizeIndex").toInt(),
        pitchValue = body.num("pitchValue"),
        lengthMm = body.num("lengthMm"),
        threadClass = body.text("threadClass"),
        material = material,
        coating = body.enumOf<CoatingType>("coating"),
        batchQty = body.num("batchQty").toInt(),
        currencyIsUsd = body.get("currencyIsUsd")?.asBoolean() ?: false,
        cncHourRateInr = body.get("cncHourRateInr")?.asDouble() ?: 750.0,
        htRatePerKgInr = body.get("htRatePerKgInr")?.asDouble() ?: 180.0,
        marginPct = body.get("marginPct")?.asDouble() ?: 22.0,
        customNominalDiaMm = body.get("customNominalDiaMm")?.takeIf { it.isNumber }?.asDouble(),
        extractedRadius = body.get("extractedRadius")?.takeIf { it.isNumber }?.asDouble(),
        propertyClass = body.get("propertyClass")?.asText()?.let { runCatching { enumValueOf<PropertyClass>(it) }.getOrNull() }
    )
}

private fun constantTimeEquals(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

private fun HttpExchange.reply(status: Int, payload: Any) {
    val bytes = mapper.writeValueAsBytes(payload)
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/health") { it.reply(200, mapOf("ok" to true)) }
    server.createContext("/v1/calculate") { ex ->
        try {
            if (ex.requestMethod != "POST") return@createContext ex.reply(405, mapOf("error" to "POST only"))
            val key = ex.requestHeaders.getFirst("X-AeroFast-Key") ?: ""
            if (apiKey != null && !constantTimeEquals(key, apiKey)) return@createContext ex.reply(401, mapOf("error" to "unauthorized"))
            val body = ex.requestBody.use { mapper.readTree(it.readNBytes(64 * 1024)) }
            ex.reply(200, mapOf("result" to calculate(body)))
        } catch (e: BadRequest) {
            ex.reply(400, mapOf("error" to e.message))
        } catch (e: Exception) {
            System.err.println("calculate failed: $e")
            ex.reply(500, mapOf("error" to "calculation failed"))
        } finally {
            ex.close()
        }
    }
    server.executor = Executors.newFixedThreadPool(8)
    server.start()
    println("AeroFast engine listening on :$port")
}
