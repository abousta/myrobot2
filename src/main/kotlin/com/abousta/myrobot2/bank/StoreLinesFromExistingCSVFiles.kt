package com.abousta.myrobot2.bank

import com.opencsv.CSVReaderBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Lit les CSV qui sont des exports de mon Relevé bancaire xlsx pro et perso
 * Et initie la bdd sqlite avec ces données (supprime toutes données existantes dans la bdd)
 * Fait une copie de sauvegarde datée de la bdd en cas d'erreur car risque de supprimer des lignes nouvelles qui n'ont pas été dans Relevés bancaires.xlsx
 */

fun main() {
    val bankLineService = BankLineService()

    // Copier fichier db backup avec date et heure et min et secondes
    val dataFolder = Paths.get("/home/abousta/progs/myrobot2/data")
    val dbFile = dataFolder.resolve("bank.db")
    val formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    Files.copy(dbFile, dataFolder.resolve("bank_$formattedDateTime.db"))

    // Init jdbc
    val dbUrl = "jdbc:sqlite:${dbFile.toAbsolutePath()}"
    val jdbcConnection = DriverManager.getConnection(dbUrl)
    jdbcConnection.autoCommit = false
    jdbcConnection.use { connection ->

        // Supprimer toutes les lignes de bank_lines
        connection.createStatement().use { statement ->
            statement.executeUpdate("DELETE FROM bank_lines")
            connection.commit()
        }

        // Lire le csv proprement
        val csvFolder = dataFolder.resolve("csv_from_excel")
        val comptePersoFile = csvFolder.resolve("compte_perso.csv")
        val compteProFile = csvFolder.resolve("compte_pro.csv")
        val bankLines = listOf(comptePersoFile, compteProFile)
            .flatMap { csv ->
                val account = csv.fileName.toString().removeSuffix(".csv")
                Files.newBufferedReader(csv).use { reader ->
                    CSVReaderBuilder(reader)
                        .withCSVParser(bankLineService.excelCsvParser())
                        .withSkipLines(1)
                        .build()
                        .use { csvReader ->
                            csvReader
                                .readAll()
                                .map { values -> bankLineService.parseBankLineFromExcelCSV(account, values) }
                        }
                }
            }

        // Ecrire dans la db toutes ces lignes
        bankLineService.insertBankLines(jdbcConnection, bankLines)
    }



    // Vérifier avec plugin intellij en ouvrant bank.db

}
