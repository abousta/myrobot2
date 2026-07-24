package com.abousta.compta

@JvmInline
value class Money(val cents: Int) {

    override fun toString(): String {
        val euros = cents / 100
        val centimes = kotlin.math.abs(cents % 100)
        return "%d,%02d EUR".format(euros, centimes)
    }
}
