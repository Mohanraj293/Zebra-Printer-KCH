package com.lazymohan.zebraprinter.exception

import androidx.annotation.StringRes
import com.lazymohan.zebraprinter.R

sealed class PrinterException(@StringRes val messageRes: Int) : Exception()

class PrinterConnectionException(@StringRes messageRes: Int) : PrinterException(messageRes)

class PrinterLanguageException : PrinterException(R.string.unknown_printer_lang)

class PrinterUnknownException() : PrinterException(R.string.common_error_message)

class PrinterHardwareException(@StringRes messageRes: Int) : PrinterException(messageRes)