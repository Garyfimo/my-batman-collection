package com.garyfimo.batmancollection.data

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

class RemoteBatmanRepository(private val api: BatmanApi) : BatmanRepository {

    override suspend fun list(): BatmanListResult = try {
        BatmanListResult.Success(api.list())
    } catch (t: Throwable) {
        BatmanListResult.Failure(t)
    }

    override suspend fun get(id: String): BatmanLookup = try {
        BatmanLookup.Found(api.get(id))
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) BatmanLookup.NotFound
        else BatmanLookup.Failure(e)
    } catch (t: Throwable) {
        BatmanLookup.Failure(t)
    }
}
