package com.garyfimo.batmancollection.core.domain

enum class FigureType(val label: String) {
    Funko("Funko"),
    HotWheels("Hot Wheels"),
    Otros("Otros"),
    ;

    companion object {
        fun fromLabel(label: String?): FigureType? =
            entries.firstOrNull { it.label == label }
    }
}
