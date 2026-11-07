package com.garyfimo.batmancollection.wishlist.presentation.addwish

import com.garyfimo.batmancollection.core.domain.FigureType

data class AddWishFormState(
    val tipo: FigureType? = null,
    val nombre: String = "",
    val serie: String = "",
    val url: String = "",
    val submitAttempted: Boolean = false,
    val submitting: Boolean = false,
) {
    val tipoError: String? get() =
        if (submitAttempted && tipo == null) ERROR_REQUIRED else null
    val nombreError: String? get() =
        if (submitAttempted && nombre.isBlank()) ERROR_REQUIRED else null
    val serieError: String? get() =
        if (submitAttempted && serie.isBlank()) ERROR_REQUIRED else null
    val urlError: String? get() = when {
        !submitAttempted -> null
        url.isBlank() -> ERROR_REQUIRED
        !isValidUrl(url) -> ERROR_INVALID_URL
        else -> null
    }

    val isValid: Boolean get() =
        tipo != null &&
            nombre.isNotBlank() &&
            serie.isNotBlank() &&
            url.isNotBlank() &&
            isValidUrl(url)

    companion object {
        const val ERROR_REQUIRED = "This is a required question"
        const val ERROR_INVALID_URL = "Enter a valid URL"

        // Pure-JVM regex so this validator works in unit tests (android.util.Patterns
        // is a stub in robolectric-less local tests and NPEs). Covers http/https with
        // a host and optional port/path/query/fragment — good enough for form validation.
        private val URL_REGEX = Regex(
            pattern = "^https?://[A-Za-z0-9.-]+(:\\d+)?(/\\S*)?$",
            option = RegexOption.IGNORE_CASE,
        )

        private fun isValidUrl(value: String): Boolean =
            URL_REGEX.matches(value.trim())
    }
}
