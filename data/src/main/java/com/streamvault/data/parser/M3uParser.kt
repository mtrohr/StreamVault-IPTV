package com.streamvault.data.parser

import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Movie
import com.streamvault.domain.util.ChannelNormalizer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Robust M3U parser that handles real-world malformed playlists.
 * Parses line-by-line in a streaming fashion to handle large files.
 *
 * Supports:
 * - #EXTM3U header
 * - #EXTINF tags with attributes
 * - Group-title for categories
 * - TVG attributes (tvg-id, tvg-name, tvg-logo, tvg-chno)
 * - Tokenized / expiring URLs
 * - Broken / malformed entries (skipped gracefully)
 */
class M3uParser {

    data class M3uHeader(
        val tvgUrl: String? = null,
        val userAgent: String? = null
    )

    data class M3uEntry(
        val name: String,
        val groupTitle: String,
        val tvgId: String?,
        val tvgName: String?,
        val tvgLogo: String?,
        val tvgChno: Int?,
        val catchUp: String?,
        val catchUpDays: Int?,
        val catchUpSource: String?,
        val url: String,
        val userAgent: String? = null,
        val rating: String? = null,
        val year: String? = null,
        val genre: String? = null,
        val durationSeconds: Int? = null,
        val extraAttributes: Map<String, String> = emptyMap()
    )

    data class ParseResult(
        val header: M3uHeader,
        val entries: List<M3uEntry>
    )

    fun parse(inputStream: InputStream): ParseResult {
        val entries = mutableListOf<M3uEntry>()
        var header = M3uHeader()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var currentLine: String?
        var extinfLine: String? = null

        reader.use {
            while (reader.readLine().also { currentLine = it } != null) {
                val line = currentLine?.trim() ?: continue

                when {
                    line.startsWith("#EXTM3U") -> {
                        val attrs = extractAttributes(line)
                        header = M3uHeader(
                            tvgUrl = attrs["x-tvg-url"] ?: attrs["url-tvg"],
                            userAgent = attrs["user-agent"]
                        )
                    }
                    line.startsWith("#EXTINF:") -> {
                        extinfLine = line
                    }
                    line.startsWith("#") -> {
                        // Other directive, skip
                    }
                    line.isNotBlank() && extinfLine != null -> {
                        try {
                            val entry = parseEntry(extinfLine!!, line, header.userAgent)
                            if (entry != null) {
                                entries.add(entry)
                            }
                        } catch (_: Exception) {
                            // Skip malformed entries
                        }
                        extinfLine = null
                    }
                    line.isNotBlank() -> {
                        extinfLine = null
                    }
                }
            }
        }

        return ParseResult(header, entries)
    }

    fun parseToChannels(inputStream: InputStream, providerId: Long): List<Channel> {
        return parse(inputStream).entries.mapIndexed { index, entry ->
            Channel(
                id = index.toLong() + 1, // Will be replaced by stableId in SyncManager
                name = entry.name,
                logoUrl = entry.tvgLogo,
                groupTitle = entry.groupTitle,
                epgChannelId = entry.tvgId ?: entry.tvgName,
                number = entry.tvgChno ?: (index + 1),
                streamUrl = entry.url,
                catchUpSupported = entry.catchUp != null,
                catchUpDays = entry.catchUpDays ?: 0,
                catchUpSource = entry.catchUpSource,
                providerId = providerId,
                logicalGroupId = ChannelNormalizer.getLogicalGroupId(entry.name, providerId)
            )
        }
    }

    private fun parseEntry(extinfLine: String, url: String, globalUserAgent: String?): M3uEntry? {
        val afterColon = extinfLine.substringAfter("#EXTINF:", "")
        if (afterColon.isBlank()) return null

        val name = extractDisplayName(afterColon)
        if (name.isBlank()) return null

        val attributes = extractAttributes(extinfLine) // Pass full line for tags

        return M3uEntry(
            name = name,
            groupTitle = attributes["group-title"] ?: "Uncategorized",
            tvgId = attributes["tvg-id"]?.takeIf { it.isNotBlank() },
            tvgName = attributes["tvg-name"]?.takeIf { it.isNotBlank() },
            tvgLogo = attributes["tvg-logo"]?.takeIf { it.isNotBlank() },
            tvgChno = attributes["tvg-chno"]?.toIntOrNull(),
            catchUp = attributes["catchup"]?.takeIf { it.isNotBlank() },
            catchUpDays = attributes["catchup-days"]?.toIntOrNull(),
            catchUpSource = attributes["catchup-source"]?.takeIf { it.isNotBlank() },
            url = url,
            userAgent = attributes["user-agent"] ?: globalUserAgent,
            rating = attributes["rating"],
            year = attributes["year"],
            genre = attributes["genre"],
            durationSeconds = extractDuration(afterColon),
            extraAttributes = attributes
        )
    }

    private fun extractDuration(extinfContent: String): Int? {
        return extinfContent.substringBefore(" ").trim().toIntOrNull()
    }

    private fun extractDisplayName(extinfContent: String): String {
        var inQuotes = false
        var lastCommaIndex = -1

        for (i in extinfContent.indices) {
            when (extinfContent[i]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) lastCommaIndex = i
            }
        }

        return if (lastCommaIndex >= 0) {
            extinfContent.substring(lastCommaIndex + 1).trim()
        } else {
            extinfContent.substringAfter(" ").trim()
        }
    }

    private fun extractAttributes(content: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val regex = """([\w-]+)="([^"]*?)"""".toRegex()

        regex.findAll(content).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            attributes[key] = value
        }

        return attributes
    }

    private fun isVodEntry(entry: M3uEntry): Boolean {
        val url = entry.url.lowercase()
        val group = entry.groupTitle.lowercase()

        return url.endsWith(".mp4") ||
                url.endsWith(".mkv") ||
                url.endsWith(".avi") ||
                url.contains("/movie/") ||
                group.contains("movie") ||
                group.contains("vod") ||
                group.contains("film")
    }
}
