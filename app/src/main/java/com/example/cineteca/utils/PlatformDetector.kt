package com.example.cineteca.utils

import java.net.URL

object PlatformDetector {

    data class PlatformInfo(val name: String, val colorHex: Long)

    private val PLATFORMS = listOf(
        Pair(listOf("youtube.com", "youtu.be"), PlatformInfo("YouTube", 0xFFFF0000)),
        Pair(listOf("netflix.com"), PlatformInfo("Netflix", 0xFFE50914)),
        Pair(listOf("hbo.com", "max.com"), PlatformInfo("Max", 0xFF002BE7)),
        Pair(listOf("disneyplus.com", "disney.com"), PlatformInfo("Disney+", 0xFF113CCF)),
        Pair(listOf("primevideo.com", "amazon.com"), PlatformInfo("Prime Video", 0xFF00A8E1)),
        Pair(listOf("mubi.com"), PlatformInfo("MUBI", 0xFF404040)),
        Pair(listOf("letterboxd.com"), PlatformInfo("Letterboxd", 0xFF00C030)),
        Pair(listOf("imdb.com"), PlatformInfo("IMDb", 0xFFF5C518)),
        Pair(listOf("filmaffinity.com"), PlatformInfo("Filmaffinity", 0xFF8B0000)),
        Pair(listOf("instagram.com"), PlatformInfo("Instagram", 0xFFE1306C)),
        Pair(listOf("tiktok.com"), PlatformInfo("TikTok", 0xFF010101)),
        Pair(listOf("vimeo.com"), PlatformInfo("Vimeo", 0xFF1AB7EA)),
        Pair(listOf("twitch.tv"), PlatformInfo("Twitch", 0xFF9146FF)),
        Pair(listOf("apple.com", "tv.apple.com"), PlatformInfo("Apple TV+", 0xFF000000)),
        Pair(listOf("paramountplus.com"), PlatformInfo("Paramount+", 0xFF0064FF)),
    )

    fun detect(url: String): PlatformInfo? {
        val host = runCatching { URL(url).host.removePrefix("www.") }.getOrNull() ?: return null
        return PLATFORMS.firstOrNull { (domains, _) ->
            domains.any { domain -> domain in host }
        }?.second
    }

    fun colorFor(platformName: String): Long? =
        PLATFORMS.firstOrNull { (_, info) -> info.name == platformName }?.second?.colorHex

    fun extractYouTubeId(url: String): String? =
        Regex("(?:v=|youtu\\.be/|shorts/|embed/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
}
