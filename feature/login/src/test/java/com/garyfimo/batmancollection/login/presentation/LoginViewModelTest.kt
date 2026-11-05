package com.garyfimo.batmancollection.login.presentation

import app.cash.turbine.test
import com.garyfimo.batmancollection.login.domain.AuthRepository
import com.garyfimo.batmancollection.login.domain.AuthState
import com.garyfimo.batmancollection.login.domain.SignInResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk<AuthRepository>().also {
        every { it.state } returns MutableStateFlow<AuthState>(AuthState.SignedOut)
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `blank fields keep submit disabled`() {
        val viewModel = LoginViewModel(authRepository)

        assertFalse(viewModel.state.value.isValid)
        viewModel.onIntent(LoginIntent.EmailChanged("garyfimo@gmail.com"))
        assertFalse(viewModel.state.value.isValid)
        viewModel.onIntent(LoginIntent.PasswordChanged("hunter2"))
        assertEquals(true, viewModel.state.value.isValid)
    }

    @Test
    fun `successful sign-in emits SignedIn and resets state`() = runTest(dispatcher) {
        coEvery { authRepository.signIn("garyfimo@gmail.com", "hunter2") } returns SignInResult.Success
        val viewModel = LoginViewModel(authRepository)
        viewModel.onIntent(LoginIntent.EmailChanged("garyfimo@gmail.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("hunter2"))

        viewModel.effects.test {
            viewModel.onIntent(LoginIntent.Submitted)
            assertEquals(LoginEffect.SignedIn, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(LoginUiState(), viewModel.state.value)
    }

    @Test
    fun `failed sign-in surfaces the error without resetting fields`() = runTest(dispatcher) {
        coEvery { authRepository.signIn("garyfimo@gmail.com", "wrong") } returns
            SignInResult.Failure("The password is invalid")
        val viewModel = LoginViewModel(authRepository)
        viewModel.onIntent(LoginIntent.EmailChanged("garyfimo@gmail.com"))
        viewModel.onIntent(LoginIntent.PasswordChanged("wrong"))

        viewModel.effects.test {
            viewModel.onIntent(LoginIntent.Submitted)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.state.value
        assertEquals("garyfimo@gmail.com", state.email)
        assertEquals("wrong", state.password)
        assertEquals("The password is invalid", state.error)
        assertFalse(state.submitting)
    }
}
