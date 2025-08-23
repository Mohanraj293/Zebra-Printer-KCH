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

//
//private fun tokens(s: String): Set<String> =
//    s.lowercase()
//        .replace(Regex("[^a-z0-9/]+"), " ")
//        .split(Regex("\\s+"))
//        .filter { it.isNotBlank() }
//        .toSet()
//
//private fun numberTokens(s: String): Set<String> =
//    Regex("\\d+").findAll(s).map { it.value }.toSet()
//
//private fun codeTokens(s: String): Set<String> =
//    s.lowercase().split(Regex("[^a-z0-9]+"))
//        .filter { it.any(Char::isDigit) && it.any(Char::isLetter) }
//        .toSet()
//
//fun similarity(a: String, b: String): Double {
//    val ta = tokens(a); val tb = tokens(b)
//    val na = numberTokens(a); val nb = numberTokens(b)
//    val ca = codeTokens(a); val cb = codeTokens(b)
//
//    val jaccard = if ((ta union tb).isEmpty()) 0.0 else (ta intersect tb).size.toDouble() / (ta union tb).size
//    val numScore = if ((na union nb).isEmpty()) 0.0 else (na intersect nb).size.toDouble() / (na union nb).size
//    val codeScore = if ((ca union cb).isEmpty()) 0.0 else (ca intersect cb).size.toDouble() / (ca union cb).size
//
//    return 0.6 * jaccard + 0.25 * numScore + 0.15 * codeScore
//}
//
//fun bestMatchIndex(
//    extracted: List<ExtractedItem>,
//    targetDesc: String,
//    used: MutableSet<Int>,
//    threshold: Double = 0.35
//): Int? {
//    var bestIdx: Int? = null
//    var bestScore = threshold
//    for ((i, ex) in extracted.withIndex()) {
//        if (i in used) continue
//        val score = similarity(ex.description, targetDesc)
//        if (score > bestScore) {
//            bestScore = score
//            bestIdx = i
//        }
//    }
//    return bestIdx
//}


private fun normalize(s: String): String =
    s.lowercase()
        .replace(Regex("[^a-z0-9/]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun tokens(s: String): Set<String> =
    normalize(s).split(" ").filter { it.isNotBlank() }.toSet()

private fun numberTokens(s: String): Set<String> =
    Regex("\\d+").findAll(s).map { it.value }.toSet()

private fun dosageTokens(s: String): Set<String> =
    Regex("\\d+(?:\\.\\d+)?(mg|ml)").findAll(s.lowercase()).map { it.value }.toSet()

fun similarity(a: String, b: String): Double {
    val ta = tokens(a); val tb = tokens(b)

    // Step 1: long words overlap (≥4 chars)
    val longA = ta.filter { it.length >= 4 }.toSet()
    val longB = tb.filter { it.length >= 4 }.toSet()
    val longOverlap = longA.intersect(longB).size
    val longWordScore = if ((longA union longB).isEmpty()) 0.0
    else longOverlap.toDouble() / (longA union longB).size

    if (longOverlap == 0) return 0.0

    // Step 2: dosage (like 10mg, 0.25ml)
    val da = dosageTokens(a); val db = dosageTokens(b)
    val dosageScore = if ((da union db).isEmpty()) 0.0
    else (da intersect db).size.toDouble() / (da union db).size

    // Step 3: plain numbers (like 28, 500)
    val na = numberTokens(a); val nb = numberTokens(b)
    val numScore = if ((na union nb).isEmpty()) 0.0
    else (na intersect nb).size.toDouble() / (na union nb).size

    // Weighted priority
    return 0.7 * longWordScore + 0.2 * dosageScore + 0.1 * numScore
}

fun bestMatchIndex(
    extracted: List<ExtractedItem>,
    targetDesc: String,
    used: MutableSet<Int>,
    threshold: Double = 0.2
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
