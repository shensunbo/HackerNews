package com.example.hackernews.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface HnApiService {
    @GET("v0/beststories.json") suspend fun bestStoryIds(): List<Long>
    @GET("v0/item/{id}.json") suspend fun item(@Path("id") id: Long): HnItem?
}
