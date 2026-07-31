package com.abousta.compta.balance

import com.abousta.compta.account.AccountType
import com.abousta.compta.infrastructure.Money
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.appendText
import kotlin.io.path.readLines

@Service
class BalanceService(@Value($$"${balance_folder}") private val balanceFolder: Path) {

    // Retrouve le dernier solde enregistré du compte voulu
    fun lastBalance(accountType: AccountType): Balance {
        val balanceFile = balanceFolder.resolve("balance_${accountType.name.lowercase()}")
        val lastLine = balanceFile.readLines().last().trim()
        return fromLineToBalance(lastLine)
    }

    fun fromLineToBalance(line: String): Balance {
        val splitted = line.trim().split(' ')
        return Balance(date = LocalDate.parse(splitted[0]), amount = Money(splitted[1].toInt()))
    }

    // Rajoute une ligne au fichier de solde pour le compte voulu
    fun appendBalance(date: LocalDate, amountInCents: Int, accountType: AccountType) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val balanceFile = balanceFolder.resolve("balance_${accountType.name.lowercase()}")
        balanceFile.appendText("$dateString $amountInCents")
    }

    fun lastTwoBalances(accountType: AccountType): Pair<Balance, Balance> {
        val balanceFile = balanceFolder.resolve("balance_${accountType.name.lowercase()}")
        val allLines = balanceFile.readLines()
        val nbLines = allLines.size
        return Pair(
            fromLineToBalance(allLines[nbLines - 2]),
            fromLineToBalance(allLines[nbLines - 1])
        )
    }
}
