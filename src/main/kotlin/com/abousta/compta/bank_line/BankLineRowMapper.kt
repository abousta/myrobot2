package com.abousta.compta.bank_line

import com.abousta.compta.infrastructure.Money
import com.abousta.compta.account.AccountType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.LocalDate

@Component
class BankLineRowMapper(
    private val objectMapper: ObjectMapper
) : RowMapper<BankLine> {

    override fun mapRow(rs: ResultSet, rowNum: Int): BankLine {
        val tagsJson = rs.getString("tags")
        return BankLine(
            date = LocalDate.parse(rs.getString("date")),
            label = rs.getString("label"),
            tags = if (tagsJson == null) {
                emptySet()
            } else {
                objectMapper.readValue<Set<String>>(tagsJson)
            },
            amount = Money(rs.getInt("amount")),
            balance = rs.getInt("balance").let {
                if (rs.wasNull()) null else Money(it)
            },
            account = AccountType.valueOf(rs.getString("account"))
        )
    }
}
