package com.abousta.myrobot2.backup

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import io.github.cdimascio.dotenv.dotenv
import java.nio.file.Paths

/**
 * Sauvegarde les données de ticktick.
 * Ca n'utilise pas l'API Tick tick car cette dernière ne sauvegarde pas les commentaires
 * A la pace, utilisation de playwright pour lancer firefox et exporter une archive zip
 * Pour le login, demande le mdp maître bitwarden et va chercher le mdp confluence dans bitwarden.
 * Pré-requis : avoir installé bitwarden-cli
 * - flatpak install flathub com.bitwarden.desktop
 * Et se logger pour la première fois avec bitwarden-cli :
 * - flatpak run --command=bw com.bitwarden.desktop login (ça enverra une vérif par mail)
 */

private const val BW_TICKTICK = "ticktick.com"
private const val EMAIL = "abousta@gmail.com"

fun main() {
    val backupDir = dotenv()["BACKUP_DIR"] + "/Ticktick"

    // Récupère session BitWarden
    val sessionKey = getBWSession()

    // Récupérer mot de passe depuis Bitwarden CLI
    val ticktickPassword = runCommand(
        "get", "password", BW_TICKTICK,
        env = mapOf("BW_SESSION" to sessionKey)
    )
    if (ticktickPassword.isEmpty()) error("Impossible de récupérer le mot de passe Ticktick depuis Bitwarden")
    println("✅ Mot de passe récupéré depuis Bitwarden")


    // Lancer Playwright avec Firefox
    Playwright.create().use { playwright ->
        val browser = playwright.firefox().launch(BrowserType.LaunchOptions().setHeadless(false))
        val context = browser.newContext(Browser.NewContextOptions().setAcceptDownloads(true))
        val page = context.newPage()

        // Aller à la page de login Atlassian
        page.navigate("https://ticktick.com/signin")

        // Saisie email et mot de passe récupéré
        page.fill("input[placeholder='Email']", EMAIL)
        page.fill("input#password", ticktickPassword)
        page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign in")).click()

        // Attend d'appuyer sur entrée au cas où le login se complique avec vérif par mail
        println("⚠️ Une fois la connexion terminée, appuyer sur entrée dans la console")
        readlnOrNull()

        // Afficher le popup des paramètres
        page.navigate("https://ticktick.com/webapp/#q/all/tasks?modalType=settings")

        // Télécharger le ZIP en cliquant sur le lien "Generate Backup"
        page.waitForTimeout(2000.0)
        val download = page.waitForDownload {
            page.getByText("Generate Backup", Page.GetByTextOptions().setExact(false)).click()
            page.getByText("Generating").waitFor()
            page.waitForTimeout(1000.0)
        }

        val dest = Paths.get(backupDir).resolve("ticktick-export.csv")
        download.saveAs(dest)
        println("✔ Export Tick Tick sur : $dest")


        readlnOrNull()


        context.close()
        browser.close()
    }

}
