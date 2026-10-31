package com.garyfimo.batmancollection.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockBatmanRepositoryTest {

    private val source = listOf(
        Batman("a", "Batman", "A", "Era", "First", "Desc"),
        Batman("b", "Batman", "B", "Era", "First", "Desc"),
    )
    private val repository = MockBatmanRepository(source = source, latencyMillis = 0L)

    @Test
    fun `list returns the source`() = runTest {
        assertEquals(source, repository.list())
    }

    @Test
    fun `get returns the matching Batman`() = runTest {
        assertEquals(source[1], repository.get("b"))
    }

    @Test
    fun `get returns null for an unknown id`() = runTest {
        assertNull(repository.get("missing"))
    }
}
