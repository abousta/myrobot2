package com.abousta.compta.excel_igam

import com.abousta.compta.account.AccountType
import com.abousta.compta.bank_line.BankLineRepository
import com.abousta.compta.infrastructure.JxlUtils
import jxl.DateCell
import jxl.LabelCell
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.write.Blank
import jxl.write.Label
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class IgamExcelService(
    @Value($$"${igam_excel_folder}") private val igamExcelFolder: Path,
    private val bankLineRepository: BankLineRepository
) {
    private val folderDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val igamDateFormat = DateTimeFormatter.ofPattern("dd/MM/yy")
    private val longYearDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val monthInFrenchDateFormat = DateTimeFormatter.ofPattern("MMMM", Locale.FRENCH)
    private val year = LocalDate.now().year
    private val igamExcelFileName = "BOUSTA BANQUE EXCEL $year.xls"


    private fun fromDateToLocalDate(date: Date) = date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()


    fun fillIgamExcelService() {
        // Remplit le Excel pour l'IGAM et l'ouvre pour vérification
        println("Mise à jour du Excel de l'IGAM $igamExcelFileName ...")

        // Sauvegarde de sécurité du fichier Excel IGAM
        println("Sauvegarde de Sécurité...")
        val backupFolderPath = igamExcelFolder.resolve("backup").resolve(folderDateFormat.format(LocalDate.now()))
        Files.createDirectories(backupFolderPath)
        val igamExcelFilePath = igamExcelFolder.resolve(igamExcelFileName)
        Files.copy(igamExcelFilePath, backupFolderPath, StandardCopyOption.REPLACE_EXISTING)

        // Récupérer la dernière date du Excel IGAM
        var greatestDate = LocalDate.ofYearDay(year, 1)
        val igamWorkbook = Workbook.getWorkbook(igamExcelFilePath.toFile())
        for (sheetIndex in 0..11) {
            val sheet = igamWorkbook.getSheet(sheetIndex)
            for (rowIndex in 6..34) {
                val cell = sheet.getCell(0, rowIndex) ?: continue
                val date = when (cell) {
                    is DateCell -> fromDateToLocalDate(cell.date)
                    is LabelCell -> try {
                        LocalDate.parse(cell.contents, igamDateFormat)
                    } catch (_: Exception) {
                        try {
                            LocalDate.parse(cell.contents, longYearDateFormat)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    else -> null
                }
                if (date == null) continue

                if (greatestDate == null || date.isAfter(greatestDate)) greatestDate = date

            }
        }
        igamWorkbook.close()
        if (greatestDate != null) {
            println("dernière date du Excel de l'IGAM : $greatestDate")
        }

        // Récupération des relevés pros après cette date
        val bankLinesToAdd = bankLineRepository.findLastProBankLines(greatestDate)
        println("Lignes à ajouter dans le Excel : ")
        bankLinesToAdd.forEach { println(it) }


        // REPORTE CES DONNÉES DANS LE EXCEL IGAM
        val ws = WorkbookSettings().apply { encoding = "ISO-8859-1" }
        val workbook = Workbook.getWorkbook(igamExcelFilePath.toFile(), ws)
        val igamExcelNewFileName = igamExcelFileName.replace(".xls", "-NEW.xls")
        val newIgamExcelWorkbook =
            Workbook.createWorkbook(igamExcelFolder.resolve(igamExcelNewFileName).toFile(), workbook)

        // Boucle sur les relevés à reporter
        bankLinesToAdd.forEach { line ->
            // Récupération du mois
            val date = line.date
            val monthString = monthInFrenchDateFormat.format(date)

            // Récupération de l'onglet de ce mois
            val sheet = newIgamExcelWorkbook.getSheet(monthString.uppercase())
            //     Ecrire à la première ligne vide à partir de 7
            (6..33).forEach { rowIndex ->
                val cell = sheet.getCell(0, rowIndex)
                if (cell == null || cell is jxl.biff.EmptyCell || cell is jxl.read.biff.BlankCell || cell is Blank) {
                    // Remplissage de la date
                    val excelLabel = Label(0, rowIndex, longYearDateFormat.format(line.date))
                    sheet.addCell(excelLabel)
                    // Banque pro ou perso ?
                    val accountName = if (line.account == AccountType.PRO) "BANQUE PRO" else "COMPTE PERSO"
                    sheet.addCell(Label(JxlUtils.getColPos("D"), rowIndex, accountName))

                    // Remplissage du libellé et du montant
                    val label = line.label.uppercase()
                    var amountNumber = line.amount.cents
                    if (amountNumber < 0) amountNumber *= -1
                    val amountHT = amountNumber / 1.2
                    val tva = amountHT * 0.2

                    if (label.contains("VIREMENT POUR MR BOUSTA TAO")) {
                        sheet.addCell(Label(1, rowIndex, "BOUSTA TAO"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("AB"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.contains("AMINE") && amountNumber == 45000) {
                        sheet.addCell(Label(1, rowIndex, "BOUSTA"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("O"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("PRELEVEMENT DE MAAF SANTE")) {
                        sheet.addCell(Label(1, rowIndex, "MAAF"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("R"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("VIREMENT DE MAAF SANTE")) {
                        sheet.addCell(Label(1, rowIndex, "MAAF"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("L"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("PRELEVEMENT DE FREE")) {
                        sheet.addCell(Label(1, rowIndex, "FREE"))
                        println("\n********* ATTENTION !! Ouvrir le facture de FREE pour voir le montant de la TVA et du HT à déclarer\n")
                        return@forEach
                    }

                    if (label.startsWith("VIREMENT POUR MR OU MME BOUSTA")
                        || label.startsWith("VIREMENT INSTANTANE A MR OU MME BOUSTA")
                    ) {
                        sheet.addCell(Label(1, rowIndex, "PRELEVEMENT PERSONNEL"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("M"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("VIREMENT DE DOMINIQUE DUTSCHER")) {
                        sheet.addCell(Label(1, rowIndex, "DUTSCHER"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("H"), rowIndex, amountHT))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("J"), rowIndex, tva))
                        return@forEach
                    }
                    if (label.contains("AGIPI")) {
                        sheet.addCell(Label(1, rowIndex, "AGIPI"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("R"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("FRAIS EMISSION DE VIREMENT PERMANENT")
                        || label.startsWith("COTISATION TRIMESTRIELLE")
                        || label.startsWith("COMMISSION PAIEMENT PAR CARTE")
                        || label.startsWith("COMMISSION DE MOUVEMENT")
                        || label.startsWith("COTISATION FORMULE DE COMPTE PRO")
                    ) {
                        sheet.addCell(Label(1, rowIndex, "LA BANQUE POSTALE"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("AA"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("PRELEVEMENT DE BOUYGUES TELECOM")) {
                        sheet.addCell(Label(1, rowIndex, "BOUYGUES"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("P"), rowIndex, amountHT))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("N"), rowIndex, tva))
                        return@forEach
                    }

                    if (label.startsWith("PRELEVEMENT DE URSSAF DES PAYS")
                        || label.startsWith("PRELEVEMENT DE URSSAF PAYS DE LO")
                    ) {
                        sheet.addCell(Label(1, rowIndex, "URSSAF"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("X"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("Vt vers : COMPTE BANCAIRE - MR OU MME BOUSTA AMINE (x4S032)")
                        && amountNumber == 450000
                    ) {
                        sheet.addCell(Label(1, rowIndex, "BOUSTA AMINE"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("O"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }

                    if (label.startsWith("TELEREGLEMENT DE TVA")
                        || label.startsWith("PRELEVEMENT B2B DE DGFIP")
                    ) {
                        sheet.addCell(Label(1, rowIndex, "DGFIP"))
                        sheet.addCell(jxl.write.Number(JxlUtils.getColPos("Y"), rowIndex, amountNumber.toDouble()))
                        return@forEach
                    }


                    // PAR DEFAUT
                    sheet.addCell(Label(1, rowIndex, label))
                    println("\n********* NON IDENTIFIE : $label\n")
                }
            }
        }


        // Finalisation de l'écriture
        newIgamExcelWorkbook.write()
        newIgamExcelWorkbook.close()

        // Supprimer l'ancien Igam Excel
        Files.delete(igamExcelFolder.resolve(igamExcelFileName))

        // Renommer igam excel-new.xls en Relevés bancaires.xls
        Files.move(igamExcelFolder.resolve(igamExcelNewFileName), igamExcelFolder.resolve(igamExcelFileName))

        // Ouvrir le fichier Excel Igam
        val excelFile = igamExcelFolder.resolve(igamExcelFileName)
        ProcessBuilder(
            "libreoffice",
            "-calc",
            excelFile.toString()
        ).start()
    }
}
