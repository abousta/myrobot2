package com.abousta.compta

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ComptaController(private val comptaService: ComptaService) {
    @PostMapping("/download-bank-lines")
    fun downloadBankLines() {
        comptaService.downloadBankLines()
    }

    @PostMapping("/import-new-lines")
    fun importNewLines() {
        comptaService.importNewLines()
    }
}
