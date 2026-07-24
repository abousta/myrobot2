package com.abousta.myrobot2.bank

import com.fasterxml.jackson.databind.ObjectMapper
import com.opencsv.CSVParserBuilder
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.Types
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class BankLineService {

    private val excelCsvParser = CSVParserBuilder()
        .withSeparator(',')
        .withQuoteChar('"')
        .build()

    fun excelCsvParser() = excelCsvParser

    fun insertBankLines(connection: Connection, bankLines: List<BankLine>) {
        val objectMapper = ObjectMapper()

        val sql = """
        INSERT INTO bank_lines(date, label, tags, account, amount, balance)
        VALUES (?, ?, json(?), ?, ?, ?)
    """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            for (bankLine in bankLines) {
                statement.setString(1, bankLine.date.toString())
                statement.setString(2, bankLine.label)
                statement.setString(3, objectMapper.writeValueAsString(bankLine.tags))
                statement.setString(4, bankLine.account)
                statement.setLong(5, bankLine.amount.toLong())
                bankLine.balance
                    ?.let { statement.setLong(6, it.toLong()) }
                    ?: statement.setNull(6, Types.INTEGER)
                statement.addBatch()
            }
            statement.executeBatch()
            connection.commit()
        }
    }

    /**
     * Parse les dates venant du CSV Excel.
     *
     * Formats acceptés :
     * - dd/MM/yyyy : 30/03/2020
     * - dd/MM/yy   : 30/03/20
     */
    private fun parseExcelCSVDate(value: String): LocalDate {
        val trimmedValue = value.trim()

        val formatters = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
        )

        for (formatter in formatters) {
            try {
                return LocalDate.parse(trimmedValue, formatter)
            } catch (_: DateTimeParseException) {
                // On essaie le format suivant.
            }
        }

        throw IllegalArgumentException(
            "Date CSV invalide : '$value'. Formats attendus : dd/MM/yyyy ou dd/MM/yy."
        )
    }


    /**
     * Lite des lignes du fichier CSV exporté depuis Excel relevés bancaires
     */
    fun parseBankLineFromExcelCSV(account: String, values: Array<String>): BankLine {
        require(values.size >= 10) {
            "Ligne CSV invalide : ${values.size} colonne(s) trouvée(s), 10 attendues. Contenu : ${values.joinToString("|")}"
        }

        if (values[0].isBlank()) {
            println("Ligne CSV invalide : date vide")
        }
        val date = parseExcelCSVDate(values[0].trim())

        val label = values[1].trim()

        val amount = values[2].takeIf { it.isNotBlank() }?.let { parseMoneyFromExcelCSV(it.trim()) } ?: Money(0)

        val tags = buildSet {
            values[3].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
            values[4].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
            values[5].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
            values[6].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
            values[7].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
            values[8].takeIf { it.isNotBlank() }?.let { add(it.trim()) }
        }

        val balance = values[9].takeIf { it.isNotBlank() }?.let { parseMoneyFromExcelCSV(values[9].trim()) }

        return BankLine(
            date = date,
            label = label,
            amount = amount,
            balance = balance,
            tags = tags,
            account = account
        )
    }


    /**
     * Convertit une valeur qui représente de l'argent en objet Money
     * Valable uniquement pour les exports depuis le Excel Relevés bancaires.xlsx
     */
    fun parseMoneyFromExcelCSV(value: String): Money {
        val cents = value
            .trim()
            .removeSurrounding("\"")
            .replace(",", "")
            .replace(".", "")
            .toLong()

        return Money(cents)
    }

    /**
     * Enregistre en bdd les nouvelles lignes qui ont été téléchargées du site la banque postale
     */
    fun storeNewLines() {
        /*// Récupérer la dernière date qu'il y a en bdd
        val lastDate = fetchLastDate()

        val lines = Files.lines(Paths.get(tempFolderPath).resolve("compte_perso.csv"))
            .skip(7) // Ignore l'en-tête du fichier
            .map { line ->
                val splittedLine = line.split(";")
                *//*BankLine(
                    date = LocalDate.parse(splittedLine[0].trim(), dateFormat),
                    label = splittedLine[1].trim(),
                    amount = BigDecimal(splittedLine[2].trim().replace("€", "").replace(" ", "").replace(",", ".")),
                )*//*
            }
        lines.forEach { println(it) }*/
    }
}
