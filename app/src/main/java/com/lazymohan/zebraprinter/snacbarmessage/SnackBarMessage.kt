package com.lazymohan.zebraprinter.snacbarmessage

import android.content.Context
import androidx.annotation.StringRes
import com.lazymohan.zebraprinter.R

sealed class SnackBarMessage {
    data class StringResMessage(
        @StringRes val messageRes: Int,
        val formatArgs: Array<Any> = emptyArray()
    ) : SnackBarMessage() {
        override fun equals(other: Any?): Boolean = other is StringResMessage &&
            messageRes == other.messageRes &&
            formatArgs.contentEquals(other.formatArgs)

        override fun hashCode(): Int = 31 * messageRes + formatArgs.contentHashCode()
    }

    data class StringMessage(val message: String?) : SnackBarMessage()
}

fun SnackBarMessage.stringError(context: Context): String = when (this) {
    is SnackBarMessage.StringMessage -> message ?: context.getString(R.string.common_error_message)
    is SnackBarMessage.StringResMessage -> context.getString(messageRes, *formatArgs)
}