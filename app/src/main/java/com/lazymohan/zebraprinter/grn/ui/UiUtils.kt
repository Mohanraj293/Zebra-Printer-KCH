package com.lazymohan.zebraprinter.grn.ui

import java.text.DecimalFormat

fun fmt(value: Double?): String {
    if (value == null) return "-"
    val df = DecimalFormat("#.##")
    return df.format(value)
}
