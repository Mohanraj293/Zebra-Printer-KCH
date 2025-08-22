package com.lazymohan.zebraprinter.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


data class DateTimeFormat(
    val dateFormat: String? = "yyyy-MM-dd",
    val timeFormat: String? = "HH:mm:ss"
)

class DateTimeConverter(
    private var dateTimeFormat: DateTimeFormat = DateTimeFormat(),
    ) {

    fun getDisplayDate(input: Date?): String {
        if (input == null) {
            return ""
        }
        return try {
            val simpleDateFormat = SimpleDateFormat(dateTimeFormat.dateFormat, Locale.getDefault())
            simpleDateFormat.timeZone = TimeZone.getDefault()
            simpleDateFormat.format(input)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun convertStringToDate(dateString: String): Date? {
        return try {
            val simpleDateFormat = SimpleDateFormat(dateTimeFormat.dateFormat, Locale.getDefault())
            simpleDateFormat.timeZone = TimeZone.getDefault()
            simpleDateFormat.parse(dateString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}