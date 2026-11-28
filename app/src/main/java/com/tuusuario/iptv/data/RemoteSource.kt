package com.tuusuario.iptv.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

 data class RemoteDiagnostic(
    val httpCode: Int,
    val contentType: String,
    val bodyPreview: String,
    val channels: List<ChannelEntity>,
    val message: String
)

object RemoteSource {
    fun fetch(url: String, forcedFormat: String = "AUTO"): RemoteDiagnostic {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 18000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PaMpATV/1.0 Android IPTV client")
            setRequestProperty("Accept", "application/json, application/x-mpegURL, text/plain, */*")
        }
        try {
            val code = connection.responseCode
            val type = connection.contentType ?: "desconocido"
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val fmt = forcedFormat.uppercase()
            val channels = when {
                fmt == "JSON" || (fmt == "AUTO" && (type.contains("json", true) || body.trimStart().startsWith("[") || body.trimStart().startsWith("{"))) -> parseJson(body)
                else -> com.tuusuario.iptv.parser.M3uParser.parseContent(body)
            }
            val preview = body.take(1200)
            val message = if (code in 200..299) "HTTP $code · ${channels.size} canales detectados" else "HTTP $code · respuesta del servidor"
            return RemoteDiagnostic(code, type, preview, channels, message)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseJson(raw: String): List<ChannelEntity> {
        val root = raw.trim()
        val array = when {
            root.startsWith("[") -> JSONArray(root)
            root.startsWith("{") -> {
                val obj = JSONObject(root)
                when {
                    obj.optJSONArray("channels") != null -> obj.getJSONArray("channels")
                    obj.optJSONArray("data") != null -> obj.getJSONArray("data")
                    obj.optJSONArray("results") != null -> obj.getJSONArray("results")
                    else -> JSONArray()
                }
            }
            else -> JSONArray()
        }
        val result = mutableListOf<ChannelEntity>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name").ifBlank { item.optString("title") }.ifBlank { item.optString("channel") }
            val stream = item.optString("streamUrl").ifBlank { item.optString("stream_url") }.ifBlank { item.optString("url") }.ifBlank { item.optString("link") }
            if (name.isBlank() || stream.isBlank()) continue
            val group = item.optString("group").ifBlank { item.optString("category") }.ifBlank { "General" }
            val logo = item.optString("logo").ifBlank { item.optString("image") }.takeIf { it.isNotBlank() }
            val id = item.optString("id").ifBlank { "json-$i-${stream.hashCode()}" }
            result += ChannelEntity(id = id, name = name, url = stream, groupName = group, logo = logo)
        }
        return result
    }
}
