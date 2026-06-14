package com.example.cineteca.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.json.JSONObject
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
            if (title != null || thumbnail.isNotEmpty()) {
                return@withContext Metadata(
                    title = title,
                    thumbnailUrl = thumbnail,
                    platform = platform
                )
            }
        }

        // 2. Open Graph scraping (funciona para Netflix, IMDb, Letterboxd, etc.)
        val og = scrapeOpenGraph(url)
        val merged = og.copy(platform = platform)

        // 3. Si tenemos título pero no imagen/descripción → Wikipedia fallback
        val titleForSearch = merged.title
        if (titleForSearch != null && (merged.thumbnailUrl == null || merged.description == null)) {
            val wiki = searchWikipedia(titleForSearch)
            return@withContext merged.copy(
                thumbnailUrl = merged.thumbnailUrl ?: wiki.thumbnailUrl,
                description = merged.description ?: wiki.description
            )
        }

        merged
    }

    // YouTube oEmbed — sin API key
    private fun fetchYouTubeTitle(url: String): String? = runCatching {
        val encoded = URLEncoder.encode(url, "UTF-8")
        val response = URL("https://www.youtube.com/oembed?url=$encoded&format=json")
            .openConnection()
            .apply {
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 6000
                readTimeout = 6000
            }
            .getInputStream()
            .bufferedReader()
            .readText()
        JSONObject(response).optString("title").takeIf { it.isNotEmpty() }
    }.getOrNull()

    // Open Graph scraping genérico
    private fun scrapeOpenGraph(url: String): Metadata = runCatching {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
            .timeout(8000)
            .followRedirects(true)
            .get()

        fun meta(vararg props: String): String? = props.firstNotNullOfOrNull { prop ->
            doc.select("meta[property=$prop]").attr("content").takeIf { it.isNotEmpty() }
                ?: doc.select("meta[name=$prop]").attr("content").takeIf { it.isNotEmpty() }
        }

        val title = meta("og:title", "twitter:title") ?: doc.title().takeIf { it.isNotEmpty() }
        val description = meta("og:description", "twitter:description", "description")
            ?.take(300)
        val image = meta("og:image", "twitter:image")

        Metadata(title = title, description = description, thumbnailUrl = image)
    }.getOrElse { Metadata() }

    // Wikipedia REST API — completamente gratuita, sin clave
    private fun searchWikipedia(query: String): Metadata = runCatching {
        // Primero buscamos el artículo
        val searchEncoded = URLEncoder.encode(query, "UTF-8")
        val searchResponse = URL("https://en.wikipedia.org/w/api.php?action=opensearch&search=$searchEncoded&limit=1&format=json")
            .openConnection()
            .apply {
                setRequestProperty("User-Agent", "CineTeca/1.0")
                connectTimeout = 5000
                readTimeout = 5000
            }
            .getInputStream()
            .bufferedReader()
            .readText()

        val searchJson = org.json.JSONArray(searchResponse)
        val titles = searchJson.optJSONArray(1) ?: return@runCatching Metadata()
        if (titles.length() == 0) return@runCatching Metadata()
        val articleTitle = URLEncoder.encode(titles.getString(0), "UTF-8")

        // Luego traemos el resumen con imagen
        val summaryResponse = URL("https://en.wikipedia.org/api/rest_v1/page/summary/$articleTitle")
            .openConnection()
            .apply {
                setRequestProperty("User-Agent", "CineTeca/1.0")
                connectTimeout = 5000
                readTimeout = 5000
            }
            .getInputStream()
            .bufferedReader()
            .readText()

        val summary = JSONObject(summaryResponse)
        val extract = summary.optString("extract").takeIf { it.isNotEmpty() }?.take(300)
        val thumbnail = summary.optJSONObject("thumbnail")?.optString("source")

        Metadata(description = extract, thumbnailUrl = thumbnail)
    }.getOrElse { Metadata() }
}
