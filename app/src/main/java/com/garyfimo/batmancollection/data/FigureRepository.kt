package com.garyfimo.batmancollection.data

interface FigureRepository {
    suspend fun list(): FigureListResult
    suspend fun get(id: String): FigureLookup
}
