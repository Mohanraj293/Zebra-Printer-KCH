package com.lazymohan.zebraprinter.inventory.data

import android.app.Application
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object OnHandCsv {

    // Read from assets/inventory/OnHandExport.csv
    fun readFromAssets(app: Application, assetPath: String): List<OnHandRow> {
        app.assets.open(assetPath).use { input ->
            BufferedReader(InputStreamReader(input)).use { br ->
                val header = br.readLine() ?: error("CSV has no header")
                val cols = splitCsv(header)

                fun idx(vararg names: String) = cols.indexOfFirst { c ->
                    names.any { n -> c.equals(n, ignoreCase = true) }
                }

                val iTxn  = idx("transaction id", "transactionid", "txn_id")
                val iLot  = idx("lot number", "lot", "batch", "batch number")
                val iExp  = idx("lot expiry", "expiry", "expiration", "exp")
                val iDesc = idx("description", "item description")
                val iGtin = idx("gtin", "barcode", "ean")

                require(iTxn >= 0 && iLot >= 0 && iExp >= 0) {
                    "CSV must include Transaction Id, Lot Number and Lot Expiry"
                }

                val out = mutableListOf<OnHandRow>()
                while (true) {
                    val line = br.readLine() ?: break
                    if (line.isBlank()) continue
                    val cells = splitCsv(line)
                    fun g(i: Int) = cells.getOrNull(i)?.trim().orEmpty()
                    val txn = g(iTxn).toLongOrNull() ?: continue
                    val lot = g(iLot)
                    val exp = normalizeExpiry(g(iExp))
                    val desc = if (iDesc >= 0) g(iDesc) else null
                    val gtin = if (iGtin >= 0) g(iGtin) else null
                    out += OnHandRow(txn, lot, exp, desc, gtin)
                }
                return out
            }
        }
    }

    private fun splitCsv(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQ = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQ && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                    else inQ = !inQ
                }
                ch == ',' && !inQ -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    // Accept YYMMDD, dd-MM-yyyy, dd/MM/yyyy, yyyy-MM-dd, MM/dd/yyyy
    private fun normalizeExpiry(raw: String): String {
        val t = raw.trim()
        if (t.matches(Regex("^\\d{6}$"))) {
            val yy = t.substring(0, 2).toInt()
            val yyyy = 2000 + yy
            val mm = t.substring(2, 4).toInt()
            val dd = t.substring(4, 6).toInt()
            return "%04d-%02d-%02d".format(yyyy, mm, dd)
        }
        val patterns = listOf("dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy")
        for (p in patterns) {
            runCatching {
                val fmt = DateTimeFormatter.ofPattern(p)
                return LocalDate.parse(t, fmt).toString()
            }
        }
        return t
    }
}
