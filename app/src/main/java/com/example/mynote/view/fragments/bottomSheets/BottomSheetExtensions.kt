package com.example.mynote.view.fragments.bottomSheets

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mynote.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Extension functions for [BottomSheetDialogFragment] to simplify configuration of
 * system bars and bottom sheet behavior.
 *
 * Overview
 * - [updateSystemBars] - Configures status bar, navigation bar, and icon colors
 * - [setUpBottomSheetBehavior] - Sets up peek height, expansion state, max height, and scrim click handling
 *
 * @see updateSystemBars
 * @see setUpBottomSheetBehavior
 */


// BottomSheet extension functions

/**
 * Configures the system bars (status bar and navigation bar) for the bottom sheet dialog.
 *
 * This function enables edge-to-edge mode for the dialog window and adjusts:
 * - Navigation bar color: transparent in landscape mode, uses surface_variant in portrait mode
 * - Status bar and navigation bar icon colors based on the current theme
 *   (dark icons for light theme, light icons for dark theme)
 *
 * This function can be called at any point after the dialog window is created.
 * Unlike [setUpBottomSheetBehavior], it does NOT depend on view measurements or insets,
 * so it can be safely called in [BottomSheetDialogFragment.onCreateDialog], [BottomSheetDialogFragment.onCreateView],
 * [BottomSheetDialogFragment.onViewCreated], or [BottomSheetDialogFragment.onStart].
 *
 * @see WindowCompat.setDecorFitsSystemWindows
 * @see androidx.core.view.WindowInsetsControllerCompat.isAppearanceLightStatusBars
 */
fun BottomSheetDialogFragment.updateSystemBars() {
    dialog?.window?.let { window ->

        // Enable full-screen mode for the dialog
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val navBarColorRes = if (isLandscape) {
            R.color.transparent  // color for landscape screen orientation
        } else {
            R.color.surface_variant   // color for portrait screen orientation
        }
        window.navigationBarColor = ContextCompat.getColor(requireContext(), navBarColorRes)

        val controller = WindowCompat.getInsetsController(window, window.decorView)

        // Determine current theme (light/dark mode)
        val isDayMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

        // true = dark icons (for light mode), false = light icons (for dark mode)
        controller.isAppearanceLightStatusBars = isDayMode
        controller.isAppearanceLightNavigationBars = isDayMode
    }
}

/**
 * Configures the bottom sheet behavior, visual appearance, and handles outside clicks.
 *
 * This function sets up:
 * - Peek height: 450 pixels (partially visible when collapsed)
 * - State: [BottomSheetBehavior.STATE_EXPANDED] - fully open, NOT partially visible
 * - Swipe to dismiss: disabled ([BottomSheetBehavior.isHideable] = false) - user cannot close by swiping down
 * - Maximum height: dynamically calculated as screen height minus status bar height,
 *   preventing the sheet from overlapping with the status bar when fully expanded
 * - Handles window insets to adjust padding for keyboard
 * - CoordinatorLayout: stretched to full screen and background color set to [R.color.color_bottom_sheet_scrim]
 * - Outside click (click on Scrim): handles sheet closing using the provided callback function
 *
 * The maximum height is updated dynamically using [android.view.WindowInsets] to adapt to
 * configuration changes (e.g., keyboard visibility, orientation change).
 *
 * **Keyboard handling:**
 * When the keyboard is visible, adds bottom padding equal to the keyboard height
 * to the FrameLayout container that holds the content of a BottomSheetDialogFragment,
 * preventing content from being hidden behind the keyboard.
 * When the keyboard is hidden, removes the padding.
 *
 * Should be called in [BottomSheetDialogFragment.onStart] after the dialog window is fully initialized.
 *
 * @param onCancelClick Callback invoked when the user taps outside the bottom sheet
 *                      (on the scrim area). Typically used to dismiss the dialog
 *                      or show a save confirmation dialog.
 *
 * @see BottomSheetBehavior
 * @see BottomSheetBehavior.STATE_EXPANDED
 * @see android.view.WindowInsets
 */
fun BottomSheetDialogFragment.setUpBottomSheetBehavior(onCancelClick: () -> Unit) {
    val bottomSheet = dialog?.findViewById<FrameLayout>(
        com.google.android.material.R.id.design_bottom_sheet
    ) ?: return

    val behavior = BottomSheetBehavior.from(bottomSheet)
    behavior.apply {

        peekHeight = 450
        isHideable = false //can it be hidden with a swipe or not
        state = BottomSheetBehavior.STATE_EXPANDED
    }

    // Setting the maximum height of the bottom sheet and padding for keyboard using insets
    ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { bottomSheet, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

        val availableMaxHeight = screenHeight - systemBars.top
        behavior.maxHeight = availableMaxHeight

        bottomSheet.post {
        if (imeVisible) {
            bottomSheet.setPadding(0, 0, 0, imeHeight)
        }
        else {
            bottomSheet.setPadding(0, 0, 0, 0)
        }}

        insets
    }
    ViewCompat.requestApplyInsets(bottomSheet)

    // Getting the root CoordinatorLayout
    val coordinator = dialog?.findViewById<CoordinatorLayout>(
        com.google.android.material.R.id.coordinator
    )
    coordinator?.setBackgroundColor(
        ContextCompat.getColor(
            requireContext(),
            R.color.color_bottom_sheet_scrim
        )
    )

    //Setting the CoordinatorLayout height to the full screen for subsequent correct calculation of maxHeight.
    coordinator?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

    // Background behind our FrameLayout with custom content bottom sheet
    val scrim = coordinator?.findViewById<View>(com.google.android.material.R.id.touch_outside)

    // Handling a click outside a dialog
    scrim?.setOnClickListener {
        onCancelClick.invoke()
    }
}