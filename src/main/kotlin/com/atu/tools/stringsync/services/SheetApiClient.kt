package com.atu.tools.stringsync.services

import com.atu.tools.stringsync.model.SheetPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class SheetApiClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()

    fun fetch(url: String): SheetPayload {
        val request = HttpRequest.newBuilder(URI(url)).GET().timeout(Duration.ofSeconds(30)).build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("HTTP ${response.statusCode()} while loading $url")
        }
        val root = json.parseToJsonElement(response.body()) as? JsonObject ?: error("Invalid JSON payload")
        val data = mutableMapOf<String, Map<String, String>>()
        for ((locale, value) in root) {
            val localeMap = value as? JsonObject ?: continue
            data[locale] = localeMap.mapNotNull { (key, v) ->
                val primitive = v as? JsonPrimitive
                if (primitive == null || !primitive.isString) null else key to primitive.content
            }.toMap()
        }
        if (data.isEmpty()) error("Sheet payload is empty")
        return SheetPayload(data)
    }
}
