package com.garyfimo.batmancollection.data

import kotlinx.coroutines.flow.Flow

interface FigureRepository {
    fun observe(): Flow<FigureListResult>
    suspend fun get(id: String): FigureLookup
    suspend fun add(draft: FigureDraft): FigureAddResult
}
