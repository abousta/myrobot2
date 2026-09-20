package com.abousta.compta

import com.abousta.compta.account.AccountType
import com.abousta.compta.account.AccountType.PERSO
import com.abousta.compta.balance.BalanceService
import com.abousta.compta.bank_line.BankLine
import com.abousta.compta.bank_line.BankLineRepository
import com.abousta.compta.infrastructure.Money
import com.abousta.compta.tag.TagRule
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Playwright
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ComptaService(
    private val bankLineRepository: BankLineRepository,
    private val balanceService: BalanceService,
    @Value($$"${csv_folder}") private val csvFolder: Path,
) {
    private val csvDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private lateinit var tagRules: List<TagRule>

    @PostConstruct
    fun init() {
        val reader = object {}.javaClass.classLoader
            .getResourceAsStream("tags_rules")
            ?.bufferedReader()!!

        tagRules = reader.useLines { lines ->
            val filtered = lines
                .map(String::trim)
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toList()

            require(filtered.size % 2 == 0) {
                "Nombre impair de lignes dans tags_rules"
            }

            filtered.chunked(2).map { (conditions, tags) ->
                TagRule(
                    conditions.split(',').map(String::trim).toSet(),
                    tags.split(',').map(String::trim).toSet()
                )
            }
        }
    }

    fun importNewLines() {
        // Pour chaque type de compte...
        AccountType.entries.forEach { accountType ->

            // Dernière date ajoutée la dernière fois en db
            val startDate = bankLineRepository.fetchLastDate(accountType)

            // Liste des libellés à cette date précise (pour éviter les doublons)
            val alreadyStoredLabels = bankLineRepository.findLinesAtDate(startDate, accountType).map { it.label }


            // Lignes du csv à partir de cette date
            val newLines = Files.lines(csvFolder.resolve("compte_${accountType.name.lowercase()}.csv"))
                .skip(7) // Ignore l'en-tête du fichier
                .map { line ->
                    val splittedLine = line.split(";")
                    val label = splittedLine[1].trim()
                    val amount = Money(
                        splittedLine[2].trim().replace("€", "").replace(" ", "").replace(",", "").toInt()
                    )
                    val tags = findTags(label, amount, accountType)
                    BankLine(
                        date = LocalDate.parse(splittedLine[0].trim(), csvDateFormat),
                        label = splittedLine[1].trim(),
                        amount = amount,
                        balance = null,
                        tags = tags,
                        account = accountType
                    )
                }
                .filter { it.date > startDate || (it.date == startDate && it.label !in alreadyStoredLabels) }


            // Ajout dans la bdd
            bankLineRepository.add(newLines.toList())

        }

    }

    private fun findTags(label: String, amount: Money, accountType: AccountType): Set<String> {
        // Chercher dans la liste de règles
        tagRules.forEach { rule ->
            rule.labels.forEach {
                if (label.startsWith(it)) return rule.tags
            }
        }

        // Si non trouvé, règles spéciales

        // Bouygues
        if (label.startsWith("PRELEVEMENT DE Bouygues Telecom")) {
            if (label.contains("09xxxxx529")) return setOf("Abousta.com", "Internet")
            if (label.contains("06xxxxx045")) return setOf("Consommation récurrente", "Téléphone", "Abonnement Marie")
            if (label.contains("07xxxxx943")) return setOf("Abousta.com", "Téléphone portable")
        }

        // Intermarché
        if (label.startsWith("ACHAT CB INTERMARCHE 24")) return setOf("Essence")
        if (label.startsWith("ACHAT CB INTERMARCHE")) return setOf("Nourriture")

        // Non trouvé
        return emptySet()
    }


    fun downloadBankLines() {
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


            AccountType.entries.forEach { account ->

                // Accès au menu "Comptes"
                page.locator("#COMPTES").click()

                // Cliquer sur le bon compte
                val accountNumber = if (account == PERSO) "0979554S032" else "1465928J032"
                val frame = page.locator("#pmo-portail-iframe").contentFrame()
                frame.getByText(accountNumber).click()

                // Récupérer le solde du compte
                frame.getByText("Passées").first().waitFor()
                val solde =
                    frame.getByText("Solde opérationnel").first().locator("..").locator("..").locator("span").last()
                        .textContent().trim()
                println("solde $account = $solde")
                val balanceInCents =
                    solde.replace(" ", "").replace(" ", "").replace(",", "").replace(".", "").replace("EUR", "")
                        .replace("€", "").toInt()
                val lastBalance = balanceService.lastBalance(account)
                if (lastBalance.date.isBefore(LocalDate.now())) {
                    balanceService.appendBalance(LocalDate.now(), balanceInCents, account)
                }

                // Aller sur la page de téléchargement des opérations
                frame.getByText("Télécharger les opérations").first().click()

                // Sélectionner le compte dans la liste déroulante
                frame.locator("input#compte").waitFor()
                frame.locator("input#compte").fill(accountNumber.take(5))
                frame.getByText(accountNumber).waitFor()
                frame.getByText(accountNumber).click()

                // Mettre une date de début
                frame.locator("input#dateDebut").waitFor()
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
                val targetPath = csvFolder.resolve("compte_${account.name.lowercase()}.csv")
                download.saveAs(targetPath)
            }

        }
    }

    /**
     * Vérifie que le nouveau solde correspond bien à la somme des mouvements
     */
    fun checkBalances() {
        for (accountType in AccountType.entries) {
            // Lire dans le fichier solde la somme attendue
            val (preLastBalance, lastBalance) = balanceService.lastTwoBalances(accountType)
            val expectedSum = lastBalance.amount.cents - preLastBalance.amount.cents
            // Lire dans la bdd la somme enregistrée entre ces deux dates
            val storedSum =
                bankLineRepository.sumBetweenTwoDates(preLastBalance.date.plusDays(1), lastBalance.date, accountType)
            if (storedSum != expectedSum) {
                println(
                    "ATTENTION. Compte $accountType - Somme attendue par le relevé des soldes = ${Money(expectedSum)}. Mais somme relevée dans la bdd = ${
                        Money(
                            storedSum
                        )
                    }. Différence = ${Money((expectedSum - storedSum))}"
                )
            } else {
                println("SOLDES OK pour compte $accountType")
            }
        }

    }
}
