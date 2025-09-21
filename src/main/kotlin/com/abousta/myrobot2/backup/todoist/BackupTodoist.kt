package com.abousta.myrobot2.backup.todoist

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cdimascio.dotenv.dotenv
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.file.Paths
import kotlin.io.path.outputStream

fun main() {

    // Chargement api token depuis .env
    val dotenv = dotenv { directory = "." }
    val token = dotenv["TODOIST_TOKEN"]

    val client = OkHttpClient()

    // Récupérer la liste des backups
    val request = Request.Builder()
        .url("https://app.todoist.com/api/v1/backups")
        .header("Authorization", "Bearer $token")
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Erreur API: ${response.code}")
        // Récupérer le preier lien de la liste des backups
        val json = response.body.string()
        val mapper = ObjectMapper()
        val backupUrl = mapper.readTree(json).first()["url"].toString().removeSurrounding("\"")
        // Télécharger le backup
        val downloadRequest = Request.Builder()
            .url(backupUrl)
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(downloadRequest).execute().use { response ->
            if (!response.isSuccessful) error("Erreur téléchargement: ${response.code}")

            val body = response.body
            val outputFile = Paths.get(dotenv["BACKUP_DIR"] + "/Todoist/last-backup.zip")
            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("Backup téléchargé dans $outputFile")
        }
    }
}
