package com.abousta.compta.bank_line

import com.abousta.compta.account.AccountType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class BankLineRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
    private val rowMapper: BankLineRowMapper,
    private val objectMapper: ObjectMapper
) {
    private val sqlDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun fetchLastDate(accountType: AccountType): LocalDate {
        val sql = "SELECT max(date) as last_date from bank_lines where account='$accountType'"
        return jdbcTemplate.queryForObject<LocalDate>(sql)!!
    }

    fun findLinesAtDate(date: LocalDate, accountType: AccountType): List<BankLine> {
        val sql = "SELECT * FROM bank_lines WHERE account='$accountType' AND date='${sqlDateFormat.format(date)}'"
        return jdbcTemplate.query(sql, rowMapper)
    }

    fun add(lines: List<BankLine>) {
        for ((date, label, amount, _, tags, account) in lines) {
            namedJdbcTemplate.update(
                """
    insert into bank_lines(date, label, tags, account, amount)
    values (:date, :label, :tags, :account, :amount)
    """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("date", sqlDateFormat.format(date))
                    .addValue("label", label)
                    .addValue("tags", objectMapper.writeValueAsString(tags))
                    .addValue("account", account)
                    .addValue("amount", amount.cents)
            )
        }
    }

    /**
     * Somme des mouvements entre deux dates, toutes deux incluses
     */
    fun sumBetweenTwoDates(dateMin: LocalDate, dateMax: LocalDate, accountType: AccountType): Int {
        val dateMinString = sqlDateFormat.format(dateMin)
        val dateMaxString = sqlDateFormat.format(dateMax)
        val sql =
            "SELECT SUM(amount) FROM bank_lines WHERE date>='$dateMinString' AND date<='$dateMaxString' AND account = '$accountType'"
        return jdbcTemplate.queryForObject<Int>(sql) ?: 0
    }


}
