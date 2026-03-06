package com.abousta.myrobot2.bank

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Locator.FilterOptions
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.io.path.writeText


private const val tempFolderPath = "/home/abousta/progs/myrobot/data/compta/temp"

private const val iframeSelector = "iframe[name=\"iFrame1\"]"

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
            .click(Locator.ClickOptions().setTimeout(2000.0)) // 2s de timeout si jamais le site ne propose pas les cookies

        println("Saisir 1538082797 dans le formulaire + le code, puis Enter : ")
        readlnOrNull()

        listOf("compte_pro", "compte_perso").forEach { accountCode ->

            // Cliquer sur le bon compte
            val accountNumber = if (accountCode == "compte_perso") "0979554S032" else "1465928J032"
            val frame = page.locator(iframeSelector).contentFrame()
            frame.getByText("CCP $accountNumber").click()

            // Récupérer le solde du compte
            val solde = frame.locator("lbp-montant.montant-lg").textContent()
            println("solde $accountCode = $solde")
            Paths.get(tempFolderPath, "${accountCode}_solde").writeText(solde)

            // Aller sur la page de téléchargement des opérations
            frame.getByText("Télécharger les opérations").click()

            // Sélectionner le compte dans la liste déroulante
            frame.locator("div.ng-select-container").click()
            frame.getByText(accountNumber).click()

            // Mettre une date de début
            val date = LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            frame.locator("input#dateDebut").fill(date)

            // Cliquer sur Télécharger
            val download = page.waitForDownload {
                frame.getByText("Télécharger").click()
            }

            // Mettre le téléchargement dans le bon rép
            val targetPath = Paths.get(tempFolderPath, "$accountCode.csv")
            download.saveAs(targetPath)

            // Revenir à l'accueil pour éventuellement télécharger un deuxième compte
            page.getByRole(AriaRole.LISTITEM).filter(FilterOptions().setHasText(Pattern.compile("^Outils & services$"))).click()
            page.getByText("Accueil").click()


        }

    }
}

