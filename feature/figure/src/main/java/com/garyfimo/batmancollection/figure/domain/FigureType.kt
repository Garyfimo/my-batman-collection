package com.garyfimo.batmancollection.figure.domain

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
