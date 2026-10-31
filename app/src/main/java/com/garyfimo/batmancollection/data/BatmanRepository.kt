package com.garyfimo.batmancollection.data

class BatmanRepository(private val api: BatmanApi) {
    suspend fun list(): List<Batman> = api.list()

    suspend fun get(id: String): Batman = api.get(id)
}
