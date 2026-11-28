package com.tuusuario.iptv.parser

import com.tuusuario.iptv.data.ChannelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest

object M3uParser {

    suspend fun fromUrl(m3uUrl: String): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val rawText = URL(m3uUrl).readText()
        parseContent(rawText)
    }

    fun parseContent(content: String): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        var pendingName: String? = null
        var pendingGroup = "General"
        var pendingTvgId: String? = null
        var pendingLogo: String? = null

        content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pendingName = line.substringAfterLast(",", "Canal sin nombre").trim()
                    pendingGroup = Regex("""group-title="([^"]*)"""").find(line)?.groupValues?.get(1)
                        ?.takeIf { it.isNotBlank() } ?: "General"
                    pendingTvgId = Regex("""tvg-id="([^"]*)"""").find(line)?.groupValues?.get(1)
                        ?.takeIf { it.isNotBlank() }
                    pendingLogo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.get(1)
                        ?.takeIf { it.isNotBlank() }
                }
                !line.startsWith("#") -> {
                    val name = pendingName ?: line.substringAfterLast("/").substringBefore("?")
                    channels.add(
                        ChannelEntity(
                            id = stableId(line, name),
                            name = name,
                            url = line,
                            groupName = pendingGroup,
                            tvgId = pendingTvgId,
                            logo = pendingLogo
                        )
                    )
                    pendingName = null
                    pendingGroup = "General"
                    pendingTvgId = null
                    pendingLogo = null
                }
            }
        }
        return channels
    }

    private fun stableId(url: String, name: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$url|$name".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
