package com.example.cineteca.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MetadataFetcher {

    data class Metadata(
        val title: String? = null,
        val description: String? = null,
        val thumbnailUrl: String? = null,
        val platform: String? = null
    )

    suspend fun fetch(url: String): Metadata = withContext(Dispatchers.IO) {
        val platform = PlatformDetector.detect(url)?.name

        // 1. YouTube: thumbnail directa (sin API) + oEmbed para título
        PlatformDetector.extractYouTubeId(url)?.let { videoId ->
            val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            val title = fetchYouTubeTitle(url)
            return@withContext Metadata(
                title = title,
                thumbnailUrl = thumbnail,
                platform = platform
            )
        }

        // 2. Open Graph scraping (Netflix, IMDb, Letterboxd, Instagram, etc.)
        val og = scrapeOpenGraph(url)
        val merged = og.copy(platform = platform)

        // 3. Si tenemos título pero no imagen → Wikipedia fallback
        if (merged.title != null && merged.thumbnailUrl == null) {
            val wiki = searchWikipedia(merged.title)
            return@withContext merged.copy(
                thumbnailUrl = wiki.thumbnailUrl,
                description = merged.description ?: wiki.description
            )
        }

        merged
    }

    // YouTube oEmbed — sin API key
    private fun fetchYouTubeTitle(url: String): String? = runCatching {
        val encoded = URLEncoder.encode(url, "UTF-8")
        val conn = URL("https://www.youtube.com/oembed?url=$encoded&format=json")
            .openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 7000
            readTimeout = 7000
        }
        if (conn.responseCode != 200) return@runCatching null
        JSONObject(conn.inputStream.bufferedReader().readText()).optString("title")
            .takeIf { it.isNotBlank() }
    }.getOrNull()

    // Open Graph scraping genérico
    private fun scrapeOpenGraph(url: String): Metadata = runCatching {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
            .header("Accept-Language", "es-ES,es;q=0.9")
            .timeout(8000)
            .followRedirects(true)
            .get()

        fun meta(vararg props: String): String? = props.firstNotNullOfOrNull { prop ->
            doc.select("meta[property=$prop]").attr("content").takeIf { it.isNotEmpty() }
                ?: doc.select("meta[name=$prop]").attr("content").takeIf { it.isNotEmpty() }
        }

        val title = meta("og:title", "twitter:title")
            ?: doc.title().removePrefix("Instagram").trim().takeIf { it.isNotEmpty() }
        val description = meta("og:description", "twitter:description", "description")
            ?.take(300)
        val image = meta("og:image", "twitter:image:src", "twitter:image")

        Metadata(title = title, description = description, thumbnailUrl = image)
    }.getOrElse { Metadata() }

    // Wikipedia REST API — gratuita, sin clave
    private fun searchWikipedia(query: String): Metadata = runCatching {
        val searchEncoded = URLEncoder.encode(query, "UTF-8")
        val searchConn = URL("https://en.wikipedia.org/w/api.php?action=opensearch&search=$searchEncoded&limit=1&format=json")
            .openConnection() as HttpURLConnection
        searchConn.apply {
            setRequestProperty("User-Agent", "CineTeca/1.0")
            connectTimeout = 5000
            readTimeout = 5000
        }
        if (searchConn.responseCode != 200) return@runCatching Metadata()

        val searchJson = org.json.JSONArray(searchConn.inputStream.bufferedReader().readText())
        val titles = searchJson.optJSONArray(1) ?: return@runCatching Metadata()
        if (titles.length() == 0) return@runCatching Metadata()

        val articleTitle = URLEncoder.encode(titles.getString(0), "UTF-8")
        val summaryConn = URL("https://en.wikipedia.org/api/rest_v1/page/summary/$articleTitle")
            .openConnection() as HttpURLConnection
        summaryConn.apply {
            setRequestProperty("User-Agent", "CineTeca/1.0")
            connectTimeout = 5000
            readTimeout = 5000
        }
        if (summaryConn.responseCode != 200) return@runCatching Metadata()

        val summary = JSONObject(summaryConn.inputStream.bufferedReader().readText())
        Metadata(
            description = summary.optString("extract").takeIf { it.isNotBlank() }?.take(300),
            thumbnailUrl = summary.optJSONObject("thumbnail")?.optString("source")
        )
    }.getOrElse { Metadata() }
}
