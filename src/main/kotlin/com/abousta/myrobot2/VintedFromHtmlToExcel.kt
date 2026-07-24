package com.abousta.myrobot2

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

data class Order(
    val date: LocalDate,
    val label: String,
    val amount: String // Avec virgule
)

fun main() {
    val doc = Jsoup.parse(Paths.get("/home/abousta/progs/myrobot2/data/vinted/index.html"))
    val orders = doc.select(".cell")
        .filter { it.select(".cell-header").isEmpty() }
        .map {
            val amountInHtml = itemProp(it, "order_value").replace("EUR", "").trim()
            val amountCentsFixed = if (amountInHtml.endsWith(".0")) amountInHtml + "0" else amountInHtml
            val isSeller = itemProp(it, "seller") == "lila.bsta"
            val amountFixed = if (isSeller) amountCentsFixed else "-$amountCentsFixed"
            Order(
                date = LocalDate.parse(itemProp(it, "order_purchased").substringBefore(' ').trim()),
                label = itemProp(it, "item_title"),
                amount = amountFixed.replace(".", ",").trim()
            )
        }
    writeCsv(orders, Paths.get("/home/abousta/progs/myrobot2/data/vinted/vinted_orders.csv"))
}

fun itemProp(parentElement: Element, itemProp: String) = parentElement.select("span[itemprop=\"$itemProp\"]").text()

fun writeCsv(orders: List<Order>, output: Path) {
    Files.newBufferedWriter(output).use { writer ->
        writer.appendLine("Date,Libellé,Montant")

        orders.forEach { order ->
            writer.appendLine(
                "${order.date},${escapeCsv(order.label)},\"${order.amount}\""
            )
        }
    }
}

fun escapeCsv(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
