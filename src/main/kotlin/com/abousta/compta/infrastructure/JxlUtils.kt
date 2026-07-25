package com.abousta.compta.infrastructure

object JxlUtils {

    fun getColPos(letter: String): Int {
        if (letter.length == 2) {
            return when (letter) {
                "AA" -> 25 + 1
                "AB" -> 25 + 2
                else -> -1
            }
        } else {
            val letterPos = letter.uppercase().first().digitToInt()
            val aPos = 'A'.digitToInt()
            return letterPos - aPos
        }
    }
}
