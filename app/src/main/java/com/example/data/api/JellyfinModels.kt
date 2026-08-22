package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JellyfinSystemInfo(
    @Json(name = "ServerName") val serverName: String?,
    @Json(name = "Version") val version: String?,
    @Json(name = "Id") val id: String?,
    @Json(name = "OperatingSystem") val operatingSystem: String?
)

@JsonClass(generateAdapter = true)
data class JellyfinItemsResponse(
    @Json(name = "Items") val items: List<JellyfinItemDto> = emptyList(),
    @Json(name = "TotalRecordCount") val totalRecordCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class JellyfinItemDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?,
    @Json(name = "OriginalTitle") val originalTitle: String?,
    @Json(name = "SeriesName") val seriesName: String?,
    @Json(name = "SeriesId") val seriesId: String?,
    @Json(name = "SeasonName") val seasonName: String?,
    @Json(name = "IndexNumber") val episodeNumber: Int?,
    @Json(name = "ParentIndexNumber") val seasonNumber: Int?,
    @Json(name = "Overview") val overview: String?,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long?,
    @Json(name = "Type") val type: String?,
    @Json(name = "Genres") val genres: List<String>?,
    @Json(name = "ProductionYear") val productionYear: Int?,
    @Json(name = "CommunityRating") val communityRating: Float?,
    @Json(name = "ImageTags") val imageTags: Map<String, String>?,
    @Json(name = "BackdropImageTags") val backdropImageTags: List<String>?,
    @Json(name = "MediaSources") val mediaSources: List<JellyfinMediaSourceDto>?
)

@JsonClass(generateAdapter = true)
data class JellyfinMediaSourceDto(
    @Json(name = "Id") val id: String?,
    @Json(name = "Path") val path: String?,
    @Json(name = "Protocol") val protocol: String?,
    @Json(name = "Container") val container: String?,
    @Json(name = "DirectStreamUrl") val directStreamUrl: String?
)

@JsonClass(generateAdapter = true)
data class JellyfinUserDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String?
)
