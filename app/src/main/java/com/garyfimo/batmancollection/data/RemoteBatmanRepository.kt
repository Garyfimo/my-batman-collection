package com.garyfimo.batmancollection.data

class RemoteBatmanRepository(private val api: BatmanApi) : BatmanRepository {
    override suspend fun list(): List<Batman> = api.list()

    override suspend fun get(id: String): Batman? = runCatching { api.get(id) }.getOrNull()
}
