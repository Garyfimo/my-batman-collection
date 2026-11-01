package com.garyfimo.batmancollection.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockBatmanRepositoryTest {

    private val source = listOf(
        Batman("a", "Batman", "A", "Era", "First", "Desc", "img-a"),
        Batman("b", "Batman", "B", "Era", "First", "Desc", "img-b"),
    )
    private val repository = MockBatmanRepository(source = source, latencyMillis = 0L)

    @Test
    fun `list returns Success with the source`() = runTest {
        assertEquals(BatmanListResult.Success(source), repository.list())
    }

    @Test
    fun `get returns Found for a known id`() = runTest {
        assertEquals(BatmanLookup.Found(source[1]), repository.get("b"))
    }

    @Test
    fun `get returns NotFound for an unknown id`() = runTest {
        assertEquals(BatmanLookup.NotFound, repository.get("missing"))
    }
}
