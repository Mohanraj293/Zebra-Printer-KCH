// app/src/main/java/com/lazymohan/zebraprinter/grn/util/DateUtils.kt
package com.lazymohan.zebraprinter.grn.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.Locale

// Output format: 2026-01-30
private val ISO_OUT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH)

// Helpers for 2-digit years (pivot = 2000)
private fun reducedDdMmYy(sep: String) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("dd${sep}MM${sep}")
    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
    .toFormatter(Locale.ENGLISH)
    .withResolverStyle(ResolverStyle.STRICT)

private fun reducedMmDdYy(sep: String) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("MM${sep}dd${sep}")
    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
    .toFormatter(Locale.ENGLISH)
    .withResolverStyle(ResolverStyle.STRICT)

private fun reducedYyMmDd(sep: String) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
    .appendPattern("${sep}MM${sep}dd")
    .toFormatter(Locale.ENGLISH)
    .withResolverStyle(ResolverStyle.STRICT)

private fun reducedDdMonYy(sep: String) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("dd${sep}MMM${sep}")
    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
    .toFormatter(Locale.ENGLISH)
    .withResolverStyle(ResolverStyle.STRICT)

private fun reducedMonDdYy(sep: String) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendPattern("MMM${sep}dd${sep}")
    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
    .toFormatter(Locale.ENGLISH)
    .withResolverStyle(ResolverStyle.STRICT)

/** Parse many inputs and return ISO yyyy-MM-dd. If parsing fails, return the trimmed input. */
fun toIsoYmd(src: String): String {
    val raw = src.trim()
    if (raw.isEmpty()) return raw
    val cleaned = raw.replace(",", " ").replace(Regex("\\s+"), " ").trim()

    val strict = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,                   // uuuu-MM-dd
        DateTimeFormatter.BASIC_ISO_DATE,                   // uuuuMMdd
        DateTimeFormatter.ofPattern("uuuu/MM/dd"),
        DateTimeFormatter.ofPattern("uuuu.MM.dd"),
        DateTimeFormatter.ofPattern("dd-MM-uuuu"),
        DateTimeFormatter.ofPattern("dd/MM/uuuu"),
        DateTimeFormatter.ofPattern("dd.MM.uuuu"),
        DateTimeFormatter.ofPattern("MM-dd-uuuu"),
        DateTimeFormatter.ofPattern("MM/dd/uuuu"),
        DateTimeFormatter.ofPattern("MM.dd.uuuu"),
        DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH)
    ).map { it.withResolverStyle(ResolverStyle.STRICT).withLocale(Locale.ENGLISH) }

    val reduced = listOf(
        reducedDdMmYy("/"), reducedDdMmYy("-"), reducedDdMmYy("."),
        reducedMmDdYy("/"), reducedMmDdYy("-"), reducedMmDdYy("."),
        reducedYyMmDd("/"), reducedYyMmDd("-"), reducedYyMmDd("."),
        reducedDdMonYy("-"), reducedDdMonYy(" "),
        reducedMonDdYy("-"), reducedMonDdYy(" ")
    )

    for (fmt in strict) runCatching { return LocalDate.parse(cleaned, fmt).format(ISO_OUT) }
    for (fmt in reduced) runCatching { return LocalDate.parse(cleaned, fmt).format(ISO_OUT) }

    // fallback: day-first 2-digit year with any of .-/ as separators
    val alt = cleaned.replace('.', '/').replace('-', '/')
    val dayFirst = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d/M/")
        .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
        .toFormatter(Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT)
    runCatching { return LocalDate.parse(alt, dayFirst).format(ISO_OUT) }

    return raw
}
