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
        val extraAttributes: Map<String, String> = emptyMap()
    )

    fun parse(inputStream: InputStream): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var currentLine: String?
        var extinfLine: String? = null

        reader.use {
            while (reader.readLine().also { currentLine = it } != null) {
                val line = currentLine?.trim() ?: continue

                when {
                    line.startsWith("#EXTM3U") -> {
                        // Header line, skip
                    }
                    line.startsWith("#EXTINF:") -> {
                        extinfLine = line
                    }
                    line.startsWith("#") -> {
                        // Other directive, skip
                    }
                    line.isNotBlank() && extinfLine != null -> {
                        // This is a URL line after an #EXTINF line
                        try {
                            val entry = parseEntry(extinfLine!!, line)
                            if (entry != null) {
                                entries.add(entry)
                            }
                        } catch (_: Exception) {
                            // Skip malformed entries
                        }
                        extinfLine = null
                    }
                    line.isNotBlank() -> {
                        // URL without #EXTINF — skip or handle as bare URL
                        extinfLine = null
                    }
                }
            }
        }

        return entries
    }

    fun parseToChannels(inputStream: InputStream, providerId: Long): List<Channel> {
        return parse(inputStream).mapIndexed { index, entry ->
            Channel(
                id = index.toLong() + 1,
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

    fun parseToMovies(inputStream: InputStream, providerId: Long): List<Movie> {
        return parse(inputStream)
            .filter { isVodEntry(it) }
            .mapIndexed { index, entry ->
                Movie(
                    id = index.toLong() + 100000,
                    name = entry.name,
                    posterUrl = entry.tvgLogo,
                    categoryName = entry.groupTitle,
                    streamUrl = entry.url,
                    providerId = providerId
                )
            }
    }

    private fun parseEntry(extinfLine: String, url: String): M3uEntry? {
        // Format: #EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Channel Name
        val afterColon = extinfLine.substringAfter("#EXTINF:", "")
        if (afterColon.isBlank()) return null

        // Extract the channel name (everything after the last comma that's not in quotes)
        val name = extractDisplayName(afterColon)
        if (name.isBlank()) return null

        // Extract attributes
        val attributes = extractAttributes(afterColon)

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
            extraAttributes = attributes
        )
    }

    private fun extractDisplayName(extinfContent: String): String {
        // The display name is after the last comma that's not inside quotes
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
            // No comma found, try to use the whole thing after the duration
            extinfContent.substringAfter(" ").trim()
        }
    }

    private fun extractAttributes(extinfContent: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        // Match key="value" patterns
        val regex = """([\w-]+)="([^"]*?)"""".toRegex()

        regex.findAll(extinfContent).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            attributes[key] = value
        }

        return attributes
    }

    private fun isVodEntry(entry: M3uEntry): Boolean {
        val url = entry.url.lowercase()
        val group = entry.groupTitle.lowercase()

        // Heuristics for identifying VOD content
        return url.endsWith(".mp4") ||
                url.endsWith(".mkv") ||
                url.endsWith(".avi") ||
                url.contains("/movie/") ||
                group.contains("movie") ||
                group.contains("vod") ||
                group.contains("film")
    }
}
