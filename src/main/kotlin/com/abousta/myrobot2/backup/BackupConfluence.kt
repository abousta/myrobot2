package com.abousta.myrobot2.backup

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.github.cdimascio.dotenv.dotenv
import java.nio.file.Paths
import javax.swing.JOptionPane
import javax.swing.JPasswordField

/**
 * Sauvegarde les espaces Confluence.
 * Ca n'utilise pas l'API Confluence car celle-ci nécessite de télécharger page à page. Trop lent.
 * A la pace, utilisation de playwright pour lancer firefox et exporter les espaces
 * Pour le login, demande le mdp maître bitwarden et va chercher le mdp confluence dans bitwarden.
 * Pré-requis : avoir installé bitwarden-cli
 * - flatpak install flathub com.bitwarden.desktop
 * Et se logger pour la première fois avec bitwarden-cli :
 * - flatpak run --command=bw com.bitwarden.desktop login (ça enverra une vérif par mail)
 */
private const val CONFLUENCE_URL = "https://abousta.atlassian.net/wiki"
private const val SPACE_IDS = "Musique,Info,MaisonVoit,Family,Boustacorp"
private const val BW_CONFLUENCE = "Atlassian abousta gmail"
private const val EMAIL = "abousta@gmail.com"

/**
 * Demande le mdp maitre avec un JPanel pour cacher le mot de passe saisi
 */
fun askMasterPassword(): String {
    val passwordField = JPasswordField()
    val option = JOptionPane.showConfirmDialog(
        null,
        passwordField,
        "Mot de passe maître",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    )
    return if (option == JOptionPane.OK_OPTION) {
        String(passwordField.password) // récupère le mot de passe saisi
    } else {
        throw RuntimeException("Mot de passe maître annulé")
    }
}

/**
 * Lance une commande flatpak pour bitwarden-cli
 */
fun runCommand(vararg command: String, env: Map<String, String> = emptyMap()): String {
    val pb = ProcessBuilder("flatpak", "run", "--command=bw", "com.bitwarden.desktop", *command)
        .redirectErrorStream(true)
    pb.environment().putAll(env)
    val process = pb.start()
    val output = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    return output
}

/**
 * Récupère une session existante bitwarden ou en déverouille une
 */
fun getBWSession(): String {
    // Vérifier si BW_SESSION est déjà définie
    val existingSession = System.getenv("BW_SESSION")
    if (!existingSession.isNullOrBlank()) {
        // Vérifier que la session est encore valide
        try {
            val status = runCommand("status", env = mapOf("BW_SESSION" to existingSession))
            if (status.contains("unlocked")) {
                println("✔ Session Bitwarden active, pas besoin de mot de passe maître")
                return existingSession
            }
        } catch (_: Exception) {
            println("❌ Session existante invalide, besoin de déverrouiller")
        }
    }

    // Sinon demander le mot de passe maître via popup
    val masterPassword = askMasterPassword()

    // Déverrouiller Bitwarden
    val sessionKey = runCommand("unlock", masterPassword, "--raw")
    if (sessionKey.isEmpty()) error("Impossible de déverrouiller Bitwarden")
    println("✔ Bitwarden déverrouillé avec succès")
    System.setProperty("BW_SESSION", sessionKey)
    return sessionKey
}

fun main() {
    val backupDir = dotenv()["BACKUP_DIR"]+"/Confluence"

    // Récupère session BitWarden
    val sessionKey = getBWSession()

    // Récupérer mot de passe Atlassian depuis Bitwarden CLI
    val atlassianPassword = runCommand(
        "get", "password", BW_CONFLUENCE,
        env = mapOf("BW_SESSION" to sessionKey)
    )
    if (atlassianPassword.isEmpty()) error("Impossible de récupérer le mot de passe Atlassian depuis Bitwarden")
    println("✅ Mot de passe récupéré depuis Bitwarden")


    // Lancer Playwright avec Firefox
    Playwright.create().use { playwright ->
        val browser = playwright.firefox().launch(BrowserType.LaunchOptions().setHeadless(false))
        val context = browser.newContext()
        val page = context.newPage()

        // Aller à la page de login Atlassian
        page.navigate("https://id.atlassian.com/login")

        // Saisie email et mot de passe récupéré
        page.fill("input[name='username']", EMAIL)
        page.click("button#login-submit")
        page.waitForSelector("input#password") // attendre le champ password
        page.fill("input#password", atlassianPassword)
        page.click("button#login-submit")

        // Attend d'appuyer sur entrée au cas où le login se complique avec vérif par mail
        println("⚠️ Une fois la connexion terminée, appuyer sur entrée dans la console")
        readlnOrNull()


        // Boucle sur tous les espaces voulus en config
        for (spaceKey in SPACE_IDS.split(",")) {
            println("➡️ Traitement de l'espace $spaceKey ...")
            // Aller sur la page des paramètres de l'espace
            // page.navigate("$CONFLUENCE_URL/spaces/viewspacesummary.action?key=$spaceKey")

            // attendre le chargement complet
            // page.waitForLoadState(LoadState.NETWORKIDLE)

            // Cliquer sur le menu "Exporter l'espace"
            // page.click("a[href='/wiki/spaces/$spaceKey/settings/export']")

            // Aller sur la page d'export html
            page.navigate("$CONFLUENCE_URL/spaces/exportspacehtml.action?key=$spaceKey")

            // Cliquer sur le bouton "Exporter"
            page.waitForSelector("input#confirm", Page.WaitForSelectorOptions().setTimeout(60000.0))
            page.click("input#confirm")

            // Télécharger le ZIP
            val download = page.waitForDownload {
                page.click("a.space-export-download-path")
            }

            val dest = Paths.get(backupDir).resolve("${spaceKey}-export.zip")
            download.saveAs(dest)
            println("✔ Export sauvegardé : $dest")

            // Petite pause pour éviter surcharge
            page.waitForTimeout(2000.0)
        }

        context.close()
        browser.close()
    }

}
