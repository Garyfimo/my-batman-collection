package com.garyfimo.batmancollection.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.data.AuthRepository
import com.garyfimo.batmancollection.data.AuthState
import com.garyfimo.batmancollection.data.Figure
import com.garyfimo.batmancollection.data.FigureListResult
import com.garyfimo.batmancollection.data.FigureRemoveResult
import com.garyfimo.batmancollection.data.FigureRepository
import com.garyfimo.batmancollection.data.FigureType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class HomeSort(val label: String) {
    CreatedAt("Date added"),
    Name("Name"),
    Type("Type"),
    Serie("Series"),
}

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

sealed interface HomeEvent {
    data class RemoveFailed(val message: String) : HomeEvent
}

class HomeViewModel(
    private val repository: FigureRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { result ->
                val previous = _state.value as? HomeUiState.Ready
                _state.value = when (result) {
                    is FigureListResult.Success -> HomeUiState.Ready(
                        items = result.items,
                        filter = previous?.filter,
                        query = previous?.query.orEmpty(),
                        sort = previous?.sort ?: HomeSort.CreatedAt,
                    )
                    is FigureListResult.Failure ->
                        HomeUiState.Error(result.cause.message ?: "Unknown error")
                }
            }
        }
    }

    fun setFilter(filter: FigureType?) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = current.copy(filter = filter)
    }

    fun setQuery(query: String) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = current.copy(query = query)
    }

    fun setSort(sort: HomeSort) {
        val current = _state.value as? HomeUiState.Ready ?: return
        _state.value = current.copy(sort = sort)
    }

    fun remove(id: String) {
        viewModelScope.launch {
            when (val result = repository.remove(id)) {
                is FigureRemoveResult.Success -> Unit // snapshot flow refreshes the list
                is FigureRemoveResult.Failure ->
                    _events.send(HomeEvent.RemoveFailed(result.cause.message ?: "Delete failed"))
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
