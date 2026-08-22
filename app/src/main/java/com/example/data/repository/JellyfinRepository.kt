package com.example.data.repository

import com.example.data.api.JellyfinApiService
import com.example.data.api.JellyfinItemDto
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class JellyfinRepository(
    private var config: ServerConfig
) {
    private var apiService: JellyfinApiService? = null
    private var currentBaseUrl: String = ""
    private var cachedUserId: String? = null

    init {
        rebuildClient(config.serverUrl)
    }

    fun updateConfig(newConfig: ServerConfig) {
        this.config = newConfig
        cachedUserId = null
        rebuildClient(newConfig.serverUrl)
    }

    private fun formatUrl(rawUrl: String): String {
        var formatted = rawUrl.trim()
        if (formatted.isBlank()) return ""
        if (!formatted.startsWith("http://", ignoreCase = true) && !formatted.startsWith("https://", ignoreCase = true)) {
            formatted = "http://$formatted"
        }
        return if (formatted.endsWith("/")) formatted else "$formatted/"
    }

    private fun rebuildClient(baseUrl: String) {
        try {
            val normalizedUrl = formatUrl(baseUrl)
            if (normalizedUrl.isBlank()) {
                apiService = null
                return
            }
            if (normalizedUrl == currentBaseUrl && apiService != null) return

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            // Relaxed TrustManager for self-signed certificates on local LAN instances
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, SecureRandom())
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(JellyfinApiService::class.java)
            currentBaseUrl = normalizedUrl
        } catch (e: Exception) {
            apiService = null
        }
    }

    private suspend fun getOrDiscoverUserId(): String? = withContext(Dispatchers.IO) {
        if (!cachedUserId.isNullOrBlank()) return@withContext cachedUserId
        val service = apiService ?: return@withContext null
        if (config.apiKey.isBlank()) return@withContext null

        try {
            val response = service.getUsers(config.apiKey, config.apiKey)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val foundId = response.body()!!.firstOrNull()?.id
                if (!foundId.isNullOrBlank()) {
                    cachedUserId = foundId
                    return@withContext foundId
                }
            }
        } catch (e: Exception) {
            // Ignore error in user discovery
        }
        null
    }

    suspend fun testConnection(): ConnectionState = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext ConnectionState.Error("Invalid server URL: ${config.serverUrl}")
        if (config.apiKey.isBlank()) {
            return@withContext ConnectionState.Error("API Key is required to connect to Jellyfin")
        }

        try {
            val response = service.getSystemInfo(config.apiKey, config.apiKey)
            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!
                // Pre-fetch user ID in background
                getOrDiscoverUserId()
                ConnectionState.Connected(
                    serverName = info.serverName ?: "Jellyfin Server",
                    version = info.version ?: "10.8+",
                    isLocal = true
                )
            } else {
                ConnectionState.Error("Server responded with HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            ConnectionState.Error("Cannot reach Jellyfin at ${config.serverUrl}: ${e.localizedMessage ?: "Connection timed out"}")
        }
    }

    suspend fun fetchMovies(libraryId: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext emptyList()
        if (config.apiKey.isBlank()) return@withContext emptyList()

        val userId = getOrDiscoverUserId()
        val targetParentId = libraryId?.takeIf { it.isNotBlank() } ?: config.moviesLibraryId.takeIf { it.isNotBlank() }

        try {
            // 1. Try with discovered user ID if available
            if (userId != null) {
                val userResponse = service.getUserItems(
                    userId = userId,
                    apiKey = config.apiKey,
                    parentId = targetParentId,
                    includeItemTypes = "Movie",
                    apiKeyParam = config.apiKey
                )
                if (userResponse.isSuccessful && !userResponse.body()?.items.isNullOrEmpty()) {
                    return@withContext userResponse.body()!!.items.map {
                        it.toMovieMediaItem(config.serverUrl, config.apiKey)
                    }
                }
            }

            // 2. Try generic Items query
            val genericResponse = service.getItems(
                apiKey = config.apiKey,
                userId = userId,
                parentId = targetParentId,
                includeItemTypes = "Movie",
                apiKeyParam = config.apiKey
            )
            if (genericResponse.isSuccessful && !genericResponse.body()?.items.isNullOrEmpty()) {
                return@withContext genericResponse.body()!!.items.map {
                    it.toMovieMediaItem(config.serverUrl, config.apiKey)
                }
            }

            // 3. Fallback: query without parentId constraint if parentId yielded 0 items
            if (targetParentId != null) {
                val globalResponse = if (userId != null) {
                    service.getUserItems(userId = userId, apiKey = config.apiKey, includeItemTypes = "Movie", apiKeyParam = config.apiKey)
                } else {
                    service.getItems(apiKey = config.apiKey, includeItemTypes = "Movie", apiKeyParam = config.apiKey)
                }
                if (globalResponse.isSuccessful && !globalResponse.body()?.items.isNullOrEmpty()) {
                    return@withContext globalResponse.body()!!.items.map {
                        it.toMovieMediaItem(config.serverUrl, config.apiKey)
                    }
                }
            }
        } catch (e: Exception) {
            // Log or ignore
        }
        emptyList()
    }

    suspend fun fetchSeries(channelType: ChannelType, libraryId: String? = null): Map<String, List<MediaItem>> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext emptyMap()
        if (config.apiKey.isBlank()) return@withContext emptyMap()

        val userId = getOrDiscoverUserId()
        val targetParentId = when (channelType) {
            ChannelType.SERIES -> libraryId?.takeIf { it.isNotBlank() } ?: config.seriesLibraryId.takeIf { it.isNotBlank() }
            ChannelType.CARTOONS -> libraryId?.takeIf { it.isNotBlank() } ?: config.cartoonsLibraryId.takeIf { it.isNotBlank() }
            ChannelType.ANIME -> libraryId?.takeIf { it.isNotBlank() } ?: config.animeLibraryId.takeIf { it.isNotBlank() }
            else -> null
        }

        try {
            var seriesItems: List<JellyfinItemDto> = emptyList()

            // 1. Fetch series list (with user ID or generic)
            if (userId != null) {
                val userResponse = service.getUserItems(
                    userId = userId,
                    apiKey = config.apiKey,
                    parentId = targetParentId,
                    includeItemTypes = "Series",
                    apiKeyParam = config.apiKey
                )
                if (userResponse.isSuccessful && !userResponse.body()?.items.isNullOrEmpty()) {
                    seriesItems = userResponse.body()!!.items
                }
            }

            if (seriesItems.isEmpty()) {
                val genericResponse = service.getItems(
                    apiKey = config.apiKey,
                    userId = userId,
                    parentId = targetParentId,
                    includeItemTypes = "Series",
                    apiKeyParam = config.apiKey
                )
                if (genericResponse.isSuccessful && !genericResponse.body()?.items.isNullOrEmpty()) {
                    seriesItems = genericResponse.body()!!.items
                }
            }

            // If targetParentId was set but found no shows, fetch all series globally
            if (seriesItems.isEmpty() && targetParentId != null) {
                val globalResponse = if (userId != null) {
                    service.getUserItems(userId = userId, apiKey = config.apiKey, includeItemTypes = "Series", apiKeyParam = config.apiKey)
                } else {
                    service.getItems(apiKey = config.apiKey, includeItemTypes = "Series", apiKeyParam = config.apiKey)
                }
                if (globalResponse.isSuccessful && !globalResponse.body()?.items.isNullOrEmpty()) {
                    seriesItems = globalResponse.body()!!.items
                }
            }

            if (seriesItems.isNotEmpty()) {
                // 1. Identify Anime candidates
                val animeShows = seriesItems.filter { s ->
                    val name = s.name ?: ""
                    val overview = s.overview ?: ""
                    val genres = s.genres ?: emptyList()
                    genres.any { g ->
                        g.contains("Anime", ignoreCase = true) ||
                        g.contains("Japanese", ignoreCase = true) ||
                        g.contains("Manga", ignoreCase = true)
                    } || name.contains("Anime", ignoreCase = true) ||
                    overview.contains("anime", ignoreCase = true) ||
                    overview.contains("Japanese", ignoreCase = true)
                }

                // 2. Identify Western / General Animation candidates (excluding anime matches if separate)
                val animeIds = animeShows.map { it.id }.toSet()
                val cartoonShows = seriesItems.filter { s ->
                    val genres = s.genres ?: emptyList()
                    val isAnim = genres.any { g ->
                        g.contains("Animation", ignoreCase = true) ||
                        g.contains("Cartoon", ignoreCase = true) ||
                        g.contains("Children", ignoreCase = true) ||
                        g.contains("Kids", ignoreCase = true) ||
                        g.contains("Family", ignoreCase = true)
                    }
                    isAnim && (s.id !in animeIds || animeShows.size > 2)
                }

                // 3. Identify Live-Action / Prime TV candidates (excluding animation if live action exists)
                val animationIds = (animeShows + cartoonShows).map { it.id }.toSet()
                val liveActionShows = seriesItems.filter { it.id !in animationIds }

                // Select target show pool for this channel with fallback and rotation
                val filteredSeries = when (channelType) {
                    ChannelType.ANIME -> {
                        if (animeShows.isNotEmpty()) {
                            animeShows
                        } else {
                            // If no specific anime genre found, pick offset partition 2
                            seriesItems.filterIndexed { idx, _ -> idx % 3 == 2 }.ifEmpty {
                                seriesItems.drop(2) + seriesItems.take(2)
                            }
                        }
                    }
                    ChannelType.CARTOONS -> {
                        if (cartoonShows.isNotEmpty()) {
                            cartoonShows
                        } else {
                            // If no specific cartoon genre found, pick offset partition 1
                            seriesItems.filterIndexed { idx, _ -> idx % 3 == 1 }.ifEmpty {
                                seriesItems.drop(1) + seriesItems.take(1)
                            }
                        }
                    }
                    ChannelType.SERIES -> {
                        if (liveActionShows.isNotEmpty()) {
                            liveActionShows
                        } else {
                            // If all shows are animated, pick offset partition 0
                            seriesItems.filterIndexed { idx, _ -> idx % 3 == 0 }.ifEmpty {
                                seriesItems
                            }
                        }
                    }
                    else -> seriesItems
                }

                val showMap = mutableMapOf<String, List<MediaItem>>()
                for (series in filteredSeries.take(25)) {
                    try {
                        val epResponse = service.getEpisodesForSeries(
                            seriesId = series.id,
                            apiKey = config.apiKey,
                            userId = userId,
                            apiKeyParam = config.apiKey
                        )
                        if (epResponse.isSuccessful && !epResponse.body()?.items.isNullOrEmpty()) {
                            val episodes = epResponse.body()!!.items.map {
                                it.toEpisodeMediaItem(series, channelType, config.serverUrl, config.apiKey)
                            }
                            if (episodes.isNotEmpty()) {
                                showMap[series.id] = episodes
                            }
                        }
                    } catch (e: Exception) {
                        // ignore single show episode failure
                    }
                }

                if (showMap.isNotEmpty()) {
                    return@withContext showMap
                }
            }
        } catch (e: Exception) {
            // Return empty map on error
        }
        emptyMap()
    }

    private fun JellyfinItemDto.toMovieMediaItem(serverUrl: String, apiKey: String): MediaItem {
        val runtimeMin = runTimeTicks?.let { (it / 10_000_000L / 60L).toInt() } ?: 90
        val cleanServerUrl = formatUrl(serverUrl).trimEnd('/')
        val poster = "$cleanServerUrl/Items/$id/Images/Primary?maxWidth=600&quality=90&api_key=$apiKey"
        val backdrop = backdropImageTags?.firstOrNull()?.let {
            "$cleanServerUrl/Items/$id/Images/Backdrop/0?maxWidth=1200&quality=90&api_key=$apiKey"
        } ?: poster
        val stream = "$cleanServerUrl/Videos/$id/stream?static=true&api_key=$apiKey"

        return MediaItem(
            id = id,
            title = name ?: "Untitled Movie",
            originalTitle = originalTitle,
            overview = overview ?: "",
            runtimeMinutes = runtimeMin.coerceAtLeast(5),
            runtimeTicks = runTimeTicks ?: (runtimeMin * 60L * 10_000_000L),
            posterUrl = poster,
            backdropUrl = backdrop,
            streamUrl = stream,
            mediaType = MediaType.MOVIE,
            channelType = ChannelType.MOVIES,
            genres = genres ?: emptyList(),
            releaseYear = productionYear,
            rating = communityRating?.let { "%.1f".format(it) }
        )
    }

    private fun JellyfinItemDto.toEpisodeMediaItem(
        seriesDto: JellyfinItemDto,
        channelType: ChannelType,
        serverUrl: String,
        apiKey: String
    ): MediaItem {
        val runtimeMin = runTimeTicks?.let { (it / 10_000_000L / 60L).toInt() } ?: 25
        val cleanServerUrl = formatUrl(serverUrl).trimEnd('/')
        val poster = "$cleanServerUrl/Items/${seriesDto.id}/Images/Primary?maxWidth=600&quality=90&api_key=$apiKey"
        val backdrop = seriesDto.backdropImageTags?.firstOrNull()?.let {
            "$cleanServerUrl/Items/${seriesDto.id}/Images/Backdrop/0?maxWidth=1200&quality=90&api_key=$apiKey"
        } ?: poster
        val stream = "$cleanServerUrl/Videos/$id/stream?static=true&api_key=$apiKey"

        return MediaItem(
            id = id,
            title = name ?: "Episode $episodeNumber",
            seriesName = seriesDto.name ?: "Series",
            seriesId = seriesDto.id,
            seasonNumber = seasonNumber ?: 1,
            episodeNumber = episodeNumber ?: 1,
            overview = overview ?: seriesDto.overview ?: "",
            runtimeMinutes = runtimeMin.coerceAtLeast(5),
            runtimeTicks = runTimeTicks ?: (runtimeMin * 60L * 10_000_000L),
            posterUrl = poster,
            backdropUrl = backdrop,
            streamUrl = stream,
            mediaType = MediaType.EPISODE,
            channelType = channelType,
            genres = seriesDto.genres ?: listOf(channelType.displayName),
            releaseYear = seriesDto.productionYear,
            rating = seriesDto.communityRating?.let { "%.1f".format(it) }
        )
    }
}
