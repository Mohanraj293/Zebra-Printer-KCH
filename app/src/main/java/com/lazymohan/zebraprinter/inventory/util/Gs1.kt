package com.lazymohan.zebraprinter.inventory.util

data class Gs1Result(
    val raw: String,
    val gtin: String?,
    val lot: String?,
    val expiryYmd: String?,
    val serial: String?
)

private const val validateCheckDigit = false


fun parseGs1(raw0: String): Gs1Result? {
    val s = raw0.trim()
        .removePrefix("]Q3")
        .removePrefix("]Q1")

    var i = 0
    var gtin: String? = null
    var lot: String? = null
    var expIso: String? = null
    var serial: String? = null

    val GS = 0x1D.toChar()
    val knownAIs = setOf("01", "10", "17", "21")

    fun isTwoDigitsAt(p: Int) =
        p + 1 < s.length && s[p].isDigit() && s[p + 1].isDigit()

    fun looksLikeBracketedAI(p: Int) =
        p + 3 < s.length && s[p] == '(' && s[p + 1].isDigit() && s[p + 2].isDigit() && s[p + 3] == ')'

    fun nextAI(start: Int): Pair<String, Int>? {
        var p = start
        if (p >= s.length) return null
        return when {
            looksLikeBracketedAI(p) -> s.substring(p + 1, p + 3) to (p + 4)
            isTwoDigitsAt(p)        -> s.substring(p, p + 2) to (p + 2)
            else                    -> null
        }
    }

    fun readFixedDigits(p0: Int, count: Int): Pair<String, Int> {
        var p = p0
        val sb = StringBuilder(count)
        while (p < s.length && sb.length < count) {
            val c = s[p]
            if (c.isDigit()) { sb.append(c); p++ }
            else if (c == GS) break
            else p++ // skip stray chars
        }
        return sb.toString() to p
    }

    fun readVarField(p0: Int): Pair<String, Int> {
        var p = p0
        val start = p
        while (p < s.length) {
            val c = s[p]
            if (c == GS) break
            if (looksLikeBracketedAI(p)) break
            if (isTwoDigitsAt(p) && s.substring(p, p + 2) in knownAIs) break
            p++
        }
        return s.substring(start, p) to p
    }

    while (i < s.length) {
        val c = s[i]
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == 0x1D.toChar()) { i++; continue }

        val aiPair = nextAI(i)
        if (aiPair == null) { i++ ; continue }

        val (ai, afterAI) = aiPair
        var p = afterAI

        when (ai) {
            "01" -> {
                val (valStr, np) = readFixedDigits(p, 14)
                if (valStr.length == 14) {
                    gtin = if (validateCheckDigit) valStr.takeIf { isValidGtin14(it) } else valStr
                }
                i = np
            }
            "17" -> {
                // If bracketed, value may be up to next '('; otherwise 6 digits
                val value: String
                if (looksLikeBracketedAI(i)) {
                    val (v, np) = readVarField(p)
                    value = v
                    i = np
                } else {
                    val (v, np) = readFixedDigits(p, 6)
                    value = v
                    i = np
                }
                expIso = normalize17ToIso(value)
            }
            "10" -> {
                val (v, np) = readVarField(p)
                lot = v.trim()
                i = np
            }
            "21" -> {
                val (v, np) = readVarField(p)
                serial = v.trim()
                i = np
            }
            else -> i = p // skip unknown
        }
    }

    if (gtin == null && lot.isNullOrBlank() && expIso.isNullOrBlank() && serial.isNullOrBlank()) return null
    return Gs1Result(raw0, gtin, lot, expIso, serial)
}

private fun normalize17ToIso(exp: String): String? {
    val t = exp.trim()
    val digits = t.replace(Regex("[^0-9]"), "")

    return when (digits.length) {
        6 -> { // YYMMDD
            val yy = digits.substring(0, 2)
            val mm = digits.substring(2, 4)
            val dd = digits.substring(4, 6)
            "20$yy-$mm-$dd" // keep 00 day if present
        }
        8 -> {
            // dd-MM-yyyy or dd/MM/yyyy
            if ((t.contains('-') && t.indexOf('-') == 2) || (t.contains('/') && t.indexOf('/') == 2)) {
                val parts = t.split('-', '/')
                if (parts.size >= 3) {
                    val dd = parts[0].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[2].padStart(4, '0')
                    return "$yyyy-$mm-$dd"
                }
            }
            // yyyyMMdd (or yyyy-MM-dd stripped)
            val yyyy = digits.substring(0, 4)
            val mm = digits.substring(4, 6)
            val dd = digits.substring(6, 8)
            "$yyyy-$mm-$dd"
        }
        else -> null
    }
}

fun isValidGtin14(s: String): Boolean {
    if (!Regex("\\d{14}").matches(s)) return false
    val d = s.map { it - '0' }
    var sum = 0
    for (i in 12 downTo 0) {
        val weight = if ((12 - i) % 2 == 0) 3 else 1
        sum += d[i] * weight
    }
    val check = (10 - (sum % 10)) % 10
    return d[13] == check
}

fun extractGtinFromRaw(raw0: String?): String? {
    if (raw0.isNullOrBlank()) return null
    val raw = raw0.trim()
    runCatching { parseGs1(raw) }.getOrNull()?.gtin?.let { return it }
    val compact = raw.replace(Regex("\\s+"), "")
    Regex("(?:\\(01\\)|01)(\\d{14})").find(compact)?.groupValues?.get(1)?.let { return it }
    return Regex("\\d{14}").find(compact)?.value
}
