package com.lazymohan.zebraprinter.grn.util

/**
 * Minimal GS1 QR detection & parsing helpers.
 * Recognizes (01) GTIN, (17) Expiry (YYMMDD), (10) Lot, (21) Serial.
 * Accepts bracketed AIs "(01)" and FNC1 (ASCII 29) separated segments.
 */
data class Gs1Result(
    val raw: String,
    val gtin: String?,
    val lot: String?,
    val expiryYmd: String?, // YYYY-MM-DD from (17)
    val serial: String?
)

private val GS = 29.toChar() // ASCII Group Separator (FNC1)

private const val validateCheckDigit = false  // set true for production

fun parseGs1(raw0: String): Gs1Result? {
    val s = raw0.trim()
        .removePrefix("]Q3")
        .removePrefix("]Q1")

    val gtin = Regex("(?:\\(01\\)|01)(\\d{14})").find(s)?.groupValues?.get(1)?.let { candidate ->
        if (validateCheckDigit) {
            candidate.takeIf { isValidGtin14(it) }
        } else candidate
    } ?: return null
    val expRaw = Regex("(?:\\(17\\)|17)([0-9\\-]{6,10})").find(s)?.groupValues?.get(1)
    val expiryYmd = expRaw?.let { exp ->
        val digits = exp.replace("-", "")
        if (digits.length == 6) {
            val yy = digits.substring(0, 2)
            val mm = digits.substring(2, 4)
            val dd = digits.substring(4, 6)
            "20$yy-$mm-$dd"
        } else null
    }
    val lot = Regex("(?:\\(10\\)|10)([^$GS()]{1,20})").find(s)?.groupValues?.get(1)
    val serial = Regex("(?:\\(21\\)|21)([^$GS()]{1,20})").find(s)?.groupValues?.get(1)
    return Gs1Result(raw0, gtin = gtin, lot = lot, expiryYmd = expiryYmd, serial = serial)
}

fun isValidGtin14(s: String): Boolean {
    if (!Regex("\\d{14}").matches(s)) return false
    val d = s.map { it - '0' }
    // Compute from right (exclude check digit at index 13). Pattern 3,1,3,1,...
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

    // 1) Try GS1 parse
    runCatching { parseGs1(raw) }.getOrNull()?.gtin?.let { return it }

    val compact = raw.replace(Regex("\\s+"), "")

    // 2) Explicit (01) + 14 digits
    Regex("(?:\\(01\\)|01)(\\d{14})").find(compact)?.groupValues?.get(1)?.let { c ->
        return c
    }

    // 3) Any 14-digit run
    Regex("\\d{14}").findAll(compact).forEach { m ->
        val cand = m.value
        return cand
    }

    return null
}
