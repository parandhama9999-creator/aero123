package com.example.util

/**
 * Centralized error handling for user-friendly messages
 */
object ErrorHandler {

    fun getUserMessage(error: Exception?, context: String = ""): String {
        return when {
            error == null -> "An unknown error occurred. Please try again."

            error.message?.contains("API key", ignoreCase = true) == true ->
                "Gemini API key not configured. Please set your key in Settings → AI Settings."

            error.message?.contains("quota", ignoreCase = true) == true ->
                "API quota exceeded. Please try again later or check your API plan."

            error.message?.contains("401", ignoreCase = true) == true ||
            error.message?.contains("403", ignoreCase = true) == true ->
                "Invalid or expired API key. Please check your key in Settings."

            error.message?.contains("429", ignoreCase = true) == true ->
                "Too many requests. Please wait a moment and try again."

            error.message?.contains("timeout", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                "Network timeout. Check your internet connection and try again."

            error.message?.contains("parse", ignoreCase = true) == true ||
            error.message?.contains("json", ignoreCase = true) == true ->
                "Failed to parse API response. The API may be returning unexpected data."

            error.message?.contains("socket", ignoreCase = true) == true ||
            error.message?.contains("unreachable", ignoreCase = true) == true ->
                "Unable to reach the API server. Check your internet connection."

            context.contains("scan", ignoreCase = true) ->
                "Failed to analyze blueprint. Please try a clearer image or enter details manually."

            context.contains("copilot", ignoreCase = true) ->
                "AI Co-Pilot is offline. Try again or use pre-roll calculations."

            else -> error.message ?: "An error occurred. Please try again."
        }
    }

    fun getLogMessage(error: Exception?, context: String = ""): String {
        return buildString {
            append("Error [")
            append(context)
            append("]: ")
            append(error?.javaClass?.simpleName ?: "Unknown")
            if (!error?.message.isNullOrBlank()) {
                append(" - ")
                append(error?.message?.take(200))
            }
        }
    }
}
