package com.garyfimo.batmancollection.data

interface BatmanRepository {
    suspend fun list(): BatmanListResult
    suspend fun get(id: String): BatmanLookup
}
