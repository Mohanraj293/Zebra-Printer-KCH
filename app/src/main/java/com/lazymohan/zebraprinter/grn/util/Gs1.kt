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

/** Best-effort parse; returns null if GTIN (01) is not found. */
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
