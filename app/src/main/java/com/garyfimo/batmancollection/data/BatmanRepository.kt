package com.garyfimo.batmancollection.data

interface BatmanRepository {
    suspend fun list(): List<Batman>
    suspend fun get(id: String): Batman?
}
