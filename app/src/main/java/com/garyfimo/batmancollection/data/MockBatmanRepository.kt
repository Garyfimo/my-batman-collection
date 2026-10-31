package com.garyfimo.batmancollection.data

import kotlinx.coroutines.delay

class MockBatmanRepository(
    private val source: List<Batman> = mockBatmen,
    private val latencyMillis: Long = 300L,
) : BatmanRepository {

    override suspend fun list(): List<Batman> {
        delay(latencyMillis)
        return source
    }

    override suspend fun get(id: String): Batman? {
        delay(latencyMillis)
        return source.firstOrNull { it.id == id }
    }
}
