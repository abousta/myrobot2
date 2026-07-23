package com.abousta.myrobot2.bank

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Playwright
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.writeText


const val tempFolderPath = "/home/abousta/progs/myrobot/data/compta/temp"

private const val iframeSelector = "#pmo-portail-iframe"

fun main() {
    Playwright.create().use { playwright ->
        val browser = playwright.firefox().launch(BrowserType.LaunchOptions().setHeadless(false))
        val context = browser.newContext()
        val page = context.newPage()

        // Aller à la page de connexion
        page.navigate("https://www.labanquepostale.fr/professionnels-entrepreneurs/connexion-espace-client-business.html")

        // Accépter les cookies
        page.locator("#footer_tc_privacy_button_3")
            .first()
            .click(
                Locator.ClickOptions().setTimeout(2000.0)
            ) // 2s de timeout si jamais le site ne propose pas les cookies

        println("Saisir 1538082797 dans le formulaire + le code, puis Enter : ")
        readlnOrNull()


        listOf("compte_pro", "compte_perso").forEach { accountCode ->

            // Accès au menu "Comptes"
            page.locator("#COMPTES").click()

            // Cliquer sur le bon compte
            val accountNumber = if (accountCode == "compte_perso") "0979554S032" else "1465928J032"
            val frame = page.locator(iframeSelector).contentFrame()
            frame.getByText(accountNumber).click()

            // Récupérer le solde du compte
            frame.getByText("Passées").first().waitFor()
            val solde = frame.getByText("Solde opérationnel").first().locator("..").locator("..").locator("span").last()
                .textContent().trim()
            println("solde $accountCode = $solde")
            Paths.get(tempFolderPath, "${accountCode}_solde").writeText(solde)

            // Aller sur la page de téléchargement des opérations
            frame.getByText("Télécharger les opérations").click()

            // Sélectionner le compte dans la liste déroulante
            frame.locator("input#compte").waitFor()
            frame.locator("input#compte").fill(accountNumber.take(5))
            frame.getByText(accountNumber).waitFor()
            frame.getByText(accountNumber).click()

            // Mettre une date de début
            val startDate = LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            frame.locator("input#dateDebut").fill(startDate)

            // Date de fin
            val endDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            frame.locator("input#dateFin").fill(endDate)

            // Cliquer sur "Lancer la recherche"
            frame.getByText("Lancer la recherche").click()

            // Ca charge les résultats. Cliquer sur l'icone pour télécharger ce résultat
            frame.getByText("Télécharger les opérations").waitFor()
            frame.getByText("Télécharger les opérations").click()

            // Ca change de contenu pour proposer le type de téléchargement.
            // Cliquer sur CSV
            frame.getByLabel("CSV").waitFor()
            frame.getByLabel("CSV").click()

            // Cliquer sur Télécharger
            val download = page.waitForDownload {
                frame.getByText("Télécharger le fichier").click()
            }

            // Mettre le téléchargement dans le bon rép
            val targetPath = Paths.get(tempFolderPath, "$accountCode.csv")
            download.saveAs(targetPath)
        }

    }
}

