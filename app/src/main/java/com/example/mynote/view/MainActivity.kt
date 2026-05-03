package com.example.mynote.view

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mynote.R

/**
 * Main activity that serves as the primary entry point for the application.
 *
 * Features:
 * - Manages edge-to-edge display with proper insets handling
 * - Handles status bar visibility to prevent UI obstruction
 * - Handles navigation bar visibility to prevent UI obstruction
 * - Handles keyboard visibility to prevent UI obstruction
 * - Handles color of system bars icons (status bar and navigation bar)
 * based on the current theme (light/dark mode).
 * - Hosts a NavHostFragment with navigation graph `nav_graph_main`
 *
 * The activity uses WindowInsets to detect when the keyboard is visible
 * and dynamically adds padding to the fragment container to ensure content
 * remains visible above the keyboard.
 *
 * @see AppCompatActivity
 * @see WindowInsetsCompat
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Uncomment for testing purposes only - forces dark mode
        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        setupWindowInsetsHandling()
        updateSystemBarsIconsColors()
    }

    override fun onStart() {
        super.onStart()

        setupStatusBarBackgroundView()
    }

    /**
     * Handles window insets to adjust padding for status bar, navigation bar and keyboard.
     *
     * **Status bar handling:**
     * For Android 15+ (API 35+) and Android 14 and below: top padding is NOT applied here
     * because we use a separate custom status bar background view ([R.id.statusBarBackground])
     * that is positioned and sized dynamically. This avoids double padding.
     *
     * **Navigation bar handling:**
     * Adds side padding equal to the navigation bar width to ensure content
     * doesn't draw behind the navigation bar in landscape mode for right- and left-handed users
     * for button navigation (not gestures), respecting system insets.
     *
     * **Keyboard handling:**
     * When the keyboard is visible, adds bottom padding equal to the keyboard height
     * to the fragment container, preventing content from being hidden behind the keyboard.
     * When the keyboard is hidden, removes the padding.
     */
    private fun setupWindowInsetsHandling() {
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->

            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Navigation bar padding in landscape mode for right- and left-handed users for button navigation (not gestures).
            val navigationBarWidthRight = systemBarsInsets.right
            val navigationBarWidthLeft = systemBarsInsets.left

            // Find container and apply the padding
            val activityContainer = findViewById<ViewGroup>(R.id.main)
            if (imeVisible) {

                    activityContainer.setPadding(navigationBarWidthLeft, 0, navigationBarWidthRight, imeHeight)
            } else {

                    activityContainer.setPadding(navigationBarWidthLeft, 0, navigationBarWidthRight, 0)
            }

            insets
        }
    }

    /**
     * Updates the color of system bars icons (status bar and navigation bar)
     * based on the current theme (light/dark mode).
     *
     * How it works:
     * - For light mode: sets icons to dark
     * - For dark mode: sets icons to light
     * - Applies the same logic to both status bar and navigation bar icons
    */
    private fun updateSystemBarsIconsColors() {

        val controller = WindowCompat.getInsetsController(window, window.decorView)

        // Determine current theme (light/dark mode)
        val isDayMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES

        // true = dark icons (for light mode), false = light icons (for dark mode)
        controller.isAppearanceLightStatusBars = isDayMode
        controller.isAppearanceLightNavigationBars = isDayMode
    }

    /**
     * Sets up a custom status bar background view, suitable for both Android 15+ (API 35+)
     * and versions below.
     *
     * Starting from Android 15, edge-to-edge is automatically enabled so the system status bar
     * is forced to be transparent with no option to set the color programmatically through themes.
     * We need to create our own background view and set its height to match the status bar height.
     *
     * This approach ensures:
     * - Proper background color under the status bar
     * - Correct height regardless of device or orientation
     * - Compatibility with both light and dark themes
     */
    private fun setupStatusBarBackgroundView(){

        val statusBarView = findViewById<View>(R.id.statusBarBackground)

        ViewCompat.setOnApplyWindowInsetsListener(statusBarView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.layoutParams.height = statusBarHeight
            view.requestLayout()
            insets
        }
    }
}