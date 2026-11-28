package com.tuusuario.iptv.parser

import android.util.Xml
import com.tuusuario.iptv.data.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

object XmlTvParser {

    private val formats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    )

    suspend fun fetchCurrentPrograms(epgUrl: String): Map<String, EpgProgram> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val currentByChannel = mutableMapOf<String, EpgProgram>()

            URL(epgUrl).openStream().use { input ->
                val parser = Xml.newPullParser()
                parser.setInput(input, null)

                var event = parser.eventType
                var inProgramme = false
                var channelId = ""
                var startMs = 0L
                var stopMs = 0L
                var title = ""
                var inTitle = false

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "programme" -> {
                                    inProgramme = true
                                    channelId = parser.getAttributeValue(null, "channel").orEmpty()
                                    startMs = parseXmlTvDate(parser.getAttributeValue(null, "start"))
                                    stopMs = parseXmlTvDate(parser.getAttributeValue(null, "stop"))
                                    title = ""
                                }
                                "title" -> if (inProgramme) inTitle = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inProgramme && inTitle) {
                                title += parser.text.orEmpty()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "title" -> inTitle = false
                                "programme" -> {
                                    if (inProgramme && channelId.isNotBlank() && title.isNotBlank()) {
                                        val isNow = now in startMs..stopMs
                                        if (isNow || !currentByChannel.containsKey(channelId)) {
                                            currentByChannel[channelId] = EpgProgram(
                                                channelId = channelId,
                                                title = title.trim(),
                                                startEpochMs = startMs,
                                                stopEpochMs = stopMs
                                            )
                                        }
                                    }
                                    inProgramme = false
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            currentByChannel
        }

    private fun parseXmlTvDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val clean = raw.trim()
        for (fmt in formats) {
            try {
                return fmt.parse(clean)?.time ?: continue
            } catch (_: Exception) {
            }
        }
        return 0L
    }
}
