package com.liveasadodo.conversationgenerator.util

import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Centralized error handling utility.
 * Provides consistent error display across the application.
 */
object ErrorHandler {

    /**
     * Shows a simple error message as a Toast.
     *
     * @param context The context for showing the toast
     * @param message The error message to display
     * @param isLongDuration If true, shows toast for longer duration (default: true)
     */
    fun showError(context: Context, message: String, isLongDuration: Boolean = true) {
        val duration = if (isLongDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }

    /**
     * Shows a simple success message as a Toast.
     *
     * @param context The context for showing the toast
     * @param message The success message to display
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows an error dialog with a title and message.
     *
     * @param context The context for showing the dialog
     * @param title The dialog title
     * @param message The error message
     * @param onPositive Optional callback for positive button click
     * @param positiveButtonText Text for positive button (default: "OK")
     * @param onNegative Optional callback for negative button click
     * @param negativeButtonText Optional text for negative button
     */
    fun showErrorDialog(
        context: Context,
        title: String,
        message: String,
        onPositive: (() -> Unit)? = null,
        positiveButtonText: String = "OK",
        onNegative: (() -> Unit)? = null,
        negativeButtonText: String? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onPositive?.invoke()
            }

        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText) { _, _ ->
                onNegative?.invoke()
            }
        }

        builder.show()
    }

    /**
     * Shows an API key error dialog with option to reset.
     *
     * @param context The context for showing the dialog
     * @param message The error message
     * @param onReset Callback for when user chooses to reset API key
     */
    fun showApiKeyError(
        context: Context,
        message: String,
        onReset: () -> Unit
    ) {
        showErrorDialog(
            context = context,
            title = "API Key Error",
            message = "$message\n\nWould you like to enter a new API key?",
            onPositive = onReset,
            positiveButtonText = "Yes",
            negativeButtonText = "Cancel"
        )
    }

    /**
     * Determines if an error message is related to API key issues.
     *
     * @param message The error message to check
     * @return True if the error is API key related
     */
    fun isApiKeyError(message: String): Boolean {
        return message.contains("API key", ignoreCase = true)
    }
}
