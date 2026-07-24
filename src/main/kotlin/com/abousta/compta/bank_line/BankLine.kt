package com.abousta.compta.bank_line

import com.abousta.compta.infrastructure.Money
import com.abousta.compta.account.AccountType
import java.time.LocalDate

data class BankLine(
    val date: LocalDate,
    val label: String,
    val amount: Money,
    val balance: Money?, // Null si pas de solde pour cette ligne
    val tags: Set<String>,
    val account: AccountType
)
