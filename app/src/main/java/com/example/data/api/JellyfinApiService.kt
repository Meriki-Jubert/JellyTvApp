package com.example.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface JellyfinApiService {

    @GET("System/Info")
    suspend fun getSystemInfo(
        @Header("X-Emby-Token") apiKey: String,
        @Query("api_key") apiKeyParam: String? = null
    ): Response<JellyfinSystemInfo>

    @GET("Users")
    suspend fun getUsers(
        @Header("X-Emby-Token") apiKey: String,
        @Query("api_key") apiKeyParam: String? = null
    ): Response<List<JellyfinUserDto>>

    @GET("UserViews")
    suspend fun getUserViews(
        @Header("X-Emby-Token") apiKey: String,
        @Query("userId") userId: String? = null,
        @Query("api_key") apiKeyParam: String? = null
    ): Response<JellyfinItemsResponse>

    @GET("Items")
    suspend fun getItems(
        @Header("X-Emby-Token") apiKey: String,
        @Query("UserId") userId: String? = null,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "Overview,RunTimeTicks,Genres,ProductionYear,CommunityRating,MediaSources,ImageTags,BackdropImageTags",
        @Query("Limit") limit: Int = 250,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("api_key") apiKeyParam: String? = null
    ): Response<JellyfinItemsResponse>

    @GET("Users/{userId}/Items")
    suspend fun getUserItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") apiKey: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "Overview,RunTimeTicks,Genres,ProductionYear,CommunityRating,MediaSources,ImageTags,BackdropImageTags",
        @Query("Limit") limit: Int = 250,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("api_key") apiKeyParam: String? = null
    ): Response<JellyfinItemsResponse>

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodesForSeries(
        @Path("seriesId") seriesId: String,
        @Header("X-Emby-Token") apiKey: String,
        @Query("UserId") userId: String? = null,
        @Query("Fields") fields: String = "Overview,RunTimeTicks,MediaSources,ImageTags,ParentIndexNumber,IndexNumber",
        @Query("SortBy") sortBy: String = "SortName",
        @Query("api_key") apiKeyParam: String? = null
    ): Response<JellyfinItemsResponse>
}
