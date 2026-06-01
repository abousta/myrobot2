package com.abousta.myrobot2.bank

import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Enregistre les nouvelles lignes stockées dans les cvs trouvés par ReleveLaBanquePostale
 */
fun main() {
    // Récupérer la dernière date qu'il y a en bdd
    val lastDate = fetchLastDate()

    val lines = Files.lines(Paths.get(tempFolderPath).resolve("compte_perso.csv"))
        .skip(7) // Ignore l'en-tête du fichier
        .map { line ->
            val splittedLine = line.split(";")
            /*BankLine(
                date = LocalDate.parse(splittedLine[0].trim(), dateFormat),
                label = splittedLine[1].trim(),
                amount = BigDecimal(splittedLine[2].trim().replace("€", "").replace(" ", "").replace(",", ".")),
            )*/
        }
    lines.forEach { println(it) }
}



private fun fetchLastDate(): LocalDate {
return LocalDate.now()// TODO
}
