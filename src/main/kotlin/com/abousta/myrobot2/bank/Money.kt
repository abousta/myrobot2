package com.abousta.myrobot2.bank

data class Money(val cents: Long) {
    fun toLong() = cents
}
