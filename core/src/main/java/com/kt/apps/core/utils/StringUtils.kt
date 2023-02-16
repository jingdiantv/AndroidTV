package com.kt.apps.core.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

fun String.formatDateTime(
    pattern: String,
    newPattern: String,
    locale: Locale = Locale.getDefault()
): String? {
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.parse(this)?.let {
        SimpleDateFormat(newPattern, locale).format(it)
    } ?: this
}

fun String.toDate(
    pattern: String,
    locale: Locale = Locale.getDefault(),
    isUtc: Boolean = false
): Date? {
    val formatter = SimpleDateFormat(pattern, locale)
    if (isUtc) formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.parse(this)
}

fun Date.formatDateTime(pattern: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(pattern, locale)
    formatter.timeZone = Calendar.getInstance(locale).timeZone
    return formatter.format(this)
}

fun String.findFirstNumber(): String? {
    val regex = "(\\d+)"
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(this)
    while (matcher.find()) {
        return matcher.group()
    }
    return null
}
