package com.lazymohan.zebraprinter.grn.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ExtractedItem(
    val description: String,
    val qtyDelivered: Double,
    val expiryDate: String,
    val batchNo: String
)

fun parseToIso(date: String): String {
    // Accepts: dd-MM-yyyy, dd/MM/yyyy, dd.MM.yyyy, yyyy-MM-dd, MM/dd/yyyy
    val patterns = listOf(
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "dd.MM.yyyy",
        "yyyy-MM-dd",
        "MM/dd/yyyy"
    )
    for (p in patterns) {
        runCatching {
            val fmt = DateTimeFormatter.ofPattern(p)
            return LocalDate.parse(date.trim(), fmt).toString()
        }
    }
    return date.trim()
}

fun toDoubleSafe(s: String?): Double =
    s?.trim()?.replace(",", "")?.toDoubleOrNull() ?: 0.0

private fun tokens(s: String): Set<String> =
    s.lowercase()
        .replace(Regex("[^a-z0-9/]+"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .toSet()

private fun numberTokens(s: String): Set<String> =
    Regex("\\d+").findAll(s).map { it.value }.toSet()

private fun codeTokens(s: String): Set<String> =
    s.lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.any(Char::isDigit) && it.any(Char::isLetter) }
        .toSet()

fun similarity(a: String, b: String): Double {
    val ta = tokens(a); val tb = tokens(b)
    val na = numberTokens(a); val nb = numberTokens(b)
    val ca = codeTokens(a); val cb = codeTokens(b)

    val jaccard = if ((ta union tb).isEmpty()) 0.0 else (ta intersect tb).size.toDouble() / (ta union tb).size
    val numScore = if ((na union nb).isEmpty()) 0.0 else (na intersect nb).size.toDouble() / (na union nb).size
    val codeScore = if ((ca union cb).isEmpty()) 0.0 else (ca intersect cb).size.toDouble() / (ca union cb).size

    return 0.6 * jaccard + 0.25 * numScore + 0.15 * codeScore
}

fun bestMatchIndex(
    extracted: List<ExtractedItem>,
    targetDesc: String,
    used: MutableSet<Int>,
    threshold: Double = 0.35
): Int? {
    var bestIdx: Int? = null
    var bestScore = threshold
    for ((i, ex) in extracted.withIndex()) {
        if (i in used) continue
        val score = similarity(ex.description, targetDesc)
        if (score > bestScore) {
            bestScore = score
            bestIdx = i
        }
    }
    return bestIdx
}
