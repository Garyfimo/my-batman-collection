package com.garyfimo.batmancollection.data

import kotlinx.coroutines.delay

class MockBatmanRepository(
    private val source: List<Batman> = mockBatmen,
    private val latencyMillis: Long = 300L,
) : BatmanRepository {

    override suspend fun list(): BatmanListResult {
        delay(latencyMillis)
        return BatmanListResult.Success(source)
    }

    override suspend fun get(id: String): BatmanLookup {
        delay(latencyMillis)
        val match = source.firstOrNull { it.id == id }
        return if (match != null) BatmanLookup.Found(match) else BatmanLookup.NotFound
    }
}
