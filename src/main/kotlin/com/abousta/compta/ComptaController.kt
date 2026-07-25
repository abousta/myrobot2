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

    @PostMapping("/check-balances")
    fun checkBalances() {
        comptaService.checkBalances()
    }

    @PostMapping("/fill-igam-excel")
    fun fillIgamExcel() {
        comptaService.fillIgamExcel()
    }
}
