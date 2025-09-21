package com.abousta.myrobot2.backup

import io.github.cdimascio.dotenv.dotenv
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.outputStream

private const val WORKSPACE_ID = "648d7680120abb5b98e2b92d"

fun main() {
    val dotenv = dotenv()
    val apiKey = dotenv["CLOCKIFY_API_KEY"]
    val client = OkHttpClient()

    // Semaine en cours : lundi → dimanche
    val today = LocalDate.now()
    val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
    val endOfWeek = today.with(java.time.DayOfWeek.SUNDAY)


    // Formatter ISO 8601 pour le JSON du body
    val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    val startIso = startOfWeek.atStartOfDay().atOffset(ZoneOffset.UTC)
    val endIso = endOfWeek.atTime(23, 59, 59).atOffset(ZoneOffset.UTC)

    val json = """
        {
          "dateRangeStart": "${isoFormatter.format(startIso)}",
          "dateRangeEnd": "${isoFormatter.format(endIso)}",
          "exportType": "CSV",
          "detailedFilter": {
            "page": 1,
            "pageSize": 1000
          }
        }
    """.trimIndent()

    // Construire la requête POST
    val request = Request.Builder()
        .url("https://reports.api.clockify.me/v1/workspaces/$WORKSPACE_ID/reports/detailed")
        .header("X-Api-Key", apiKey)
        .header("Content-Type", "application/json")
        .post(json.toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(request).execute().use { response ->
        // Calcul nom du fichier
        val fileFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy")
        val startStr = fileFormatter.format(startOfWeek)
        val endStr = fileFormatter.format(endOfWeek)
        val fileName = "Clockify_Time_Report_Detailed_${startStr}-${endStr}.csv"
        // Sauvegarde
        val backupFilePath = dotenv["BACKUP_DIR"] + "/Clockify/$fileName"
        if (!response.isSuccessful) error("Erreur export: ${response.code}")
        response.body.byteStream().use { input ->
            Paths.get(backupFilePath).outputStream().use { output -> input.copyTo(output) }
        }
        println("Rapport sauvegardé dans $backupFilePath")
    }


}
