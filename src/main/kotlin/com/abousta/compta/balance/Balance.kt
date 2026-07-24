package com.abousta.compta.balance

import com.abousta.compta.infrastructure.Money
import java.time.LocalDate

data class Balance(
    val date: LocalDate,
    val amount: Money
)
