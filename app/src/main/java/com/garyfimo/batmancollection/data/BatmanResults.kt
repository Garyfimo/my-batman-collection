package com.garyfimo.batmancollection.data

sealed interface BatmanListResult {
    data class Success(val items: List<Batman>) : BatmanListResult
    data class Failure(val cause: Throwable) : BatmanListResult
}

sealed interface BatmanLookup {
    data class Found(val batman: Batman) : BatmanLookup
    data object NotFound : BatmanLookup
    data class Failure(val cause: Throwable) : BatmanLookup
}
