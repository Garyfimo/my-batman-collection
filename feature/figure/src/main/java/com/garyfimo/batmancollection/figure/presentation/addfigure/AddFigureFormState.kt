package com.garyfimo.batmancollection.figure.presentation.addfigure

import com.garyfimo.batmancollection.core.domain.FigureType

data class AddFigureFormState(
    val tipo: FigureType? = null,
    val nombre: String = "",
    val serie: String = "",
    val imageUri: String? = null,
    val submitAttempted: Boolean = false,
    val submitting: Boolean = false,
) {
    val tipoError: String? get() =
        if (submitAttempted && tipo == null) ERROR_REQUIRED else null
    val nombreError: String? get() =
        if (submitAttempted && nombre.isBlank()) ERROR_REQUIRED else null
    val serieError: String? get() =
        if (submitAttempted && serie.isBlank()) ERROR_REQUIRED else null
    val imageError: String? get() =
        if (submitAttempted && imageUri.isNullOrBlank()) ERROR_REQUIRED else null

    val isValid: Boolean get() =
        tipo != null && nombre.isNotBlank() && serie.isNotBlank() && !imageUri.isNullOrBlank()

    companion object {
        const val ERROR_REQUIRED = "This is a required question"
    }
}
