package com.garyfimo.batmancollection.wishlist.presentation.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garyfimo.batmancollection.analytics.domain.Analytics
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.wishlist.domain.WishItem
import com.garyfimo.batmancollection.wishlist.domain.WishListResult
import com.garyfimo.batmancollection.wishlist.domain.WishRemoveResult
import com.garyfimo.batmancollection.wishlist.domain.WishRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val repository: WishRepository,
    authRepository: AuthRepository,
    analytics: Analytics,
) : ViewModel() {

    private val analyticEvents = WishlistAnalyticEvents(analytics)

    private val _state = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val state: StateFlow<WishlistUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state

    private val _effects = Channel<WishlistEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        analyticEvents.onScreenView()
        viewModelScope.launch {
            repository.observe().collect { result ->
                _state.value = when (result) {
                    is WishListResult.Success -> WishlistUiState.Ready(result.items)
                    is WishListResult.Failure ->
                        WishlistUiState.Error(result.cause.message ?: "Unknown error")
                }
            }
        }
    }

    fun onIntent(intent: WishlistIntent) {
        when (intent) {
            is WishlistIntent.WishSelected -> openWish(intent.wish)
            is WishlistIntent.WishRemoved -> remove(intent.wish)
        }
    }

    private fun openWish(wish: WishItem) {
        analyticEvents.onWishSelected(wish)
        if (wish.url.isNotBlank()) {
            viewModelScope.launch { _effects.send(WishlistEffect.OpenUrl(wish.url)) }
        }
    }

    private fun remove(wish: WishItem) {
        viewModelScope.launch {
            when (val result = repository.remove(wish.id)) {
                is WishRemoveResult.Success -> analyticEvents.onWishDeleted(wish)
                is WishRemoveResult.Failure ->
                    _effects.send(WishlistEffect.RemoveFailed(result.cause.message ?: "Delete failed"))
            }
        }
    }
}
