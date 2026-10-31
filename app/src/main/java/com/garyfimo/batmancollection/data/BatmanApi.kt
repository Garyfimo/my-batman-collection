package com.garyfimo.batmancollection.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class Batman(
    val id: String,
    val name: String,
    val description: String,
)

class BatmanApi(private val client: HttpClient) {
    suspend fun list(): List<Batman> = client.get("$BASE_URL/batmen").body()

    suspend fun get(id: String): Batman = client.get("$BASE_URL/batmen/$id").body()

    private companion object {
        const val BASE_URL = "https://example.com/api"
    }
}
