package com.example.mynote.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment

/**
 * Utility functions for keyboard management.
 *
 * Provides extension functions for showing/hiding the keyboard and a sophisticated
 * mechanism for showing the keyboard with focus stability checking.
 *
 * **Main features:**
 * - Simple keyboard show/hide for any View
 * - Smart keyboard showing with focus stability verification
 * - Cursor position restoration
 * - Callback on successful keyboard open
 *
 * **Usage examples:**
 * ```
 * // Simple keyboard operations
 * editText.showKeyboard()
 * editText.hideKeyboard()
 *
 * // Smart keyboard show with focus checking
 * forceShowKeyboard(
 *     view = editText,
 *     savedCursorPosition = cursorPosition,
 *     onSuccess = { Log.d("Keyboard", "Opened successfully") }
 * )
 * ```
 *
 * @see View.showKeyboard
 * @see View.hideKeyboard
 * @see Fragment.forceShowKeyboard
 */


// View extension functions

/**
 * Shows the keyboard for the calling View.
 *
 * Example: `editText.showKeyboard()`
 */
fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Hides the keyboard.
 *
 * Example: `editText.hideKeyboard()` or `view.hideKeyboard()`
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Forces the keyboard to appear, checking for focus stability after a certain amount of time.
 *
 * This function attempts to show the keyboard with a recursive delay mechanism.
 * It checks if the target EditText has focus and retries up to 6 times
 * with increasing delays (200, 400, 600, 800, 1000, 1200ms).
 *
 * **Use case:** When the keyboard doesn't open after configuration changes
 * (e.g., screen rotation) due to focus instability.
 *
 * @param view The EditText to show the keyboard for
 * @param savedCursorPosition Cursor position to restore after keyboard opens (-1 to skip)
 * @param onSuccess Optional callback invoked when the keyboard successfully opens
 *
 * * Example: `forceShowKeyboard(editText, cursorPosition)`
 *
 * @see checkFocusStabilityAndShowKeyboard
 */
fun Fragment.forceShowKeyboard(
    view: EditText,
    savedCursorPosition: Int,
    onSuccess: (() -> Unit)? = null
) {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

    // Save the cursor position locally
    val positionToRestore = savedCursorPosition

    // Instead of several attempts with fixed delays,
    // we use one, but with a stability check
    checkFocusStabilityAndShowKeyboard(view, imm, positionToRestore, attempt = 1, onSuccess)
}

/**
 * Recursively checks focus stability and shows the keyboard.
 *
 * This function is called internally by [forceShowKeyboard]. It checks if the
 * target view has stable focus and attempts to show the keyboard.
 * If focus is not yet stable, it retries with increasing delays.
 *
 * @param view The EditText to show the keyboard for
 * @param imm InputMethodManager instance
 * @param positionToRestore Cursor position to restore (-1 to skip)
 * @param attempt Current attempt number (1-6)
 * @param onSuccess Optional callback invoked on successful keyboard opening
 */
fun Fragment.checkFocusStabilityAndShowKeyboard(
    view: EditText,
    imm: InputMethodManager,
    positionToRestore: Int,
    attempt: Int,
    onSuccess: (() -> Unit)? = null
) {
    if (attempt > 6) {
        Log.d("KeyboardControl", "XXX All attempts have been exhausted XXX")
        return
    }

    val delay = when (attempt) {
        1 -> 200L
        2 -> 400L
        3 -> 600L
        4 -> 800L
        5 -> 1000L
        6 -> 1200L
        else -> 1000L
    }

    view.postDelayed({
        // Check if the fragment and view are still alive
        if (!isAdded || !view.isAttachedToWindow) {
            Log.d("KeyboardControl", "Fragment or view has been destroyed.")
            return@postDelayed
        }

        if (view.hasFocus()) {
            Log.d("KeyboardControl", "Focus is stable on attempt $attempt")

            // Restore the cursor
            if (positionToRestore != -1) {
                val textLength = view.text?.length ?: 0
                val finalPosition = if (positionToRestore <= textLength)
                    positionToRestore else textLength
                view.setSelection(finalPosition)
            }

            // Trying to open the keyboard
            val result = imm.showSoftInput(view, 0)
            Log.d("KeyboardControl", "showSoftInput result: $result")

            // Check after 300ms whether it has opened
            view.postDelayed({
                if (imm.isActive) {
                    Log.d("KeyboardControl", "Keyboard opened successfully!")
                    onSuccess?.invoke()
                } else {
                    Log.d("KeyboardControl", "XXX The keyboard didn't open, so we tried toggle XXX")
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
            }, 300)

        } else {
            Log.d("KeyboardControl", "Attempt $attempt: no focus, try again in ${delay}ms")

            // Trying to request focus
            view.requestFocus()

            // Continue checking
            checkFocusStabilityAndShowKeyboard(view, imm, positionToRestore, attempt + 1, onSuccess)
        }
    }, delay)
}