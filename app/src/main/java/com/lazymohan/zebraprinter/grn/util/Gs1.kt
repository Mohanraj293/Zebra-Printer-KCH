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
    val expiryYmd: String?, // 20YY-MM-DD from (17)
    val serial: String?
)

private val GS = 29.toChar() // ASCII Group Separator (FNC1)

/** True if string looks like GS1 (FNC1 present or (01)14digits present). */
fun isGs1Qr(raw0: String?): Boolean {
    if (raw0.isNullOrBlank()) return false
    val raw = raw0.trim()
    if (raw.contains(GS)) return true
    if (Regex("(?:\\(01\\)|01)\\d{14}").containsMatchIn(raw)) return true
    return false
}

fun parseGs1(raw0: String): Gs1Result? {
    val s = raw0.trim()
        .removePrefix("]Q3")
        .removePrefix("]Q1")

    val gtin = Regex("(?:\\(01\\)|01)(\\d{14})").find(s)?.groupValues?.get(1) ?: return null
    val expYYMMDD = Regex("(?:\\(17\\)|17)(\\d{6})").find(s)?.groupValues?.get(1)
    val lot = Regex("(?:\\(10\\)|10)([^\\(\\)$GS]{1,20})").find(s)?.groupValues?.get(1)
    val serial = Regex("(?:\\(21\\)|21)([^\\(\\)$GS]{1,20})").find(s)?.groupValues?.get(1)

    val expiryYmd = expYYMMDD?.let {
        val yy = it.substring(0, 2)
        val mm = it.substring(2, 4)
        val dd = it.substring(4, 6)
        "20$yy-$mm-$dd"
    }
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

    // 1) If it's GS1-ish, let the GS1 parser try first.
    runCatching { parseGs1(raw) }.getOrNull()?.gtin?.let { gt ->
        if (isValidGtin14(gt)) return gt
    }

    // Compact (remove whitespace) for easier matching.
    val compact = raw.replace(Regex("\\s+"), "")

    // 2) Prefer explicit AI (01) followed by 14 digits.
    Regex("(?:\\(01\\)|01)(\\d{14})").find(compact)?.groupValues?.get(1)?.let { c ->
        if (isValidGtin14(c)) return c
    }

    // 3) Fallback: any 14-digit run that is a valid GTIN-14.
    Regex("\\d{14}").findAll(compact).forEach { m ->
        val cand = m.value
        if (isValidGtin14(cand)) return cand
    }

    return null
}
