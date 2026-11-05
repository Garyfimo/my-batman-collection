package com.garyfimo.batmancollection.figure.presentation.home

import com.garyfimo.batmancollection.figure.domain.Figure
import com.garyfimo.batmancollection.figure.domain.FigureType

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val items: List<Figure>,
        val filter: FigureType? = null,
        val query: String = "",
        val sort: HomeSort = HomeSort.CreatedAt,
    ) : HomeUiState {
        val visibleItems: List<Figure> = items
            .let { base -> if (filter == null) base else base.filter { it.tipo == filter } }
            .let { byTipo ->
                val needle = query.trim()
                if (needle.isEmpty()) {
                    byTipo
                } else {
                    byTipo.filter {
                        it.nombre.contains(needle, ignoreCase = true) ||
                            it.serie.contains(needle, ignoreCase = true)
                    }
                }
            }
            .let { byFilters ->
                when (sort) {
                    // CreatedAt relies on the Firestore query's own orderBy, so the
                    // repo-provided order is already what we want — don't re-sort and
                    // risk losing stable ordering for figures with equal timestamps.
                    HomeSort.CreatedAt -> byFilters
                    HomeSort.Name -> byFilters.sortedBy { it.nombre.lowercase() }
                    HomeSort.Type -> byFilters.sortedWith(
                        compareBy({ it.tipo.ordinal }, { it.nombre.lowercase() }),
                    )
                    HomeSort.Serie -> byFilters.sortedBy { it.serie.lowercase() }
                }
            }
    }
    data class Error(val message: String) : HomeUiState
}
