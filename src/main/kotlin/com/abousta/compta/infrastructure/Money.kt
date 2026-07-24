package com.abousta.compta.infrastructure

import kotlin.math.abs

@JvmInline
value class Money(val cents: Int) {

    override fun toString(): String {
        val euros = cents / 100
        val centimes = abs(cents % 100)
        return "%d,%02d EUR".format(euros, centimes)
    }
}
