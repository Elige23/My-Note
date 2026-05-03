package com.example.mynote.utils

import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.widget.EditText

/**
 * Extension functions for [EditText] to handle touch event interception conflicts
 * with parent views like BottomSheetBehavior or ViewPager2 and adapt UI based on screen orientation.
 *
 * ## Touch Interception Functions
 * These functions prevent parent views from intercepting touch events when
 * an EditText with scrollable content is placed inside a scrollable parent
 * (BottomSheet, ViewPager2), diagonal or vertical swipes may be intercepted by
 * the parent, causing unintended behavior (e.g., BottomSheet collapsing instead
 * of text scrolling, ViewPager2 switching pages instead of scrolling text).
 *
 * ## UI Adaptation Functions
 * Handles the maximum number of lines depending on portrait or landscape mode to ensure
 * other UI elements have enough room to be used.
 *
 * @see EditText.preventBottomSheetInterceptionScrollUp
 * @see EditText.preventBottomSheetInterceptionFully
 * @see EditText.preventViewPagerInterceptionFully
 * @see EditText.adjustMaxLinesBasedOnOrientation
 */


// EditText extension functions

/**
 * Prevents the BottomSheet from collapsing when the user scrolls inside an EditText.
 *
 * When the user touches an EditText that has scrollable content, this function:
 * - Blocks the BottomSheet from intercepting touch events (disables swipe to collapse)
 * - Allows the EditText to handle vertical scrolling naturally
 * - Restores BottomSheet's ability to intercept events when the touch ends
 *
 * Why this is needed:
 * Without this, a swipe gesture on an EditText would be captured by the BottomSheetBehavior,
 * causing the BottomSheet to collapse instead of scrolling the text field's content.
 *
 * The logic:
 * - Single task description: Only blocks BottomSheet when
 *   the content can scroll UP (`canScrollVertically(-1)`). This handles the case where
 *   the user has typed enough text to require scrolling.
 *
 * Note: The EditText's touch events are still passed through (returning false), allowing
 * the original scrolling behavior to work alongside the BottomSheet blocking mechanism.
 *
 * @see View.canScrollVertically
 * @see android.view.ViewParent.requestDisallowInterceptTouchEvent
 */
fun EditText.preventBottomSheetInterceptionScrollUp() {
    this.setOnTouchListener { v, event ->
        if (v.canScrollVertically(-1)) {
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        false
    }
}

/**
 * Prevents the BottomSheet from collapsing when the user scrolls inside an EditText.
 *
 * When the user touches an EditText that has scrollable content, this function:
 * - Blocks the BottomSheet from intercepting touch events (disables swipe to collapse)
 * - Allows the EditText to handle vertical scrolling naturally
 * - Restores BottomSheet's ability to intercept events when the touch ends
 *
 * Why this is needed:
 * Without this, a swipe gesture on an EditText would be captured by the BottomSheetBehavior,
 * causing the BottomSheet to collapse instead of scrolling the text field's content.
 *
 * The logic:
 * - Group task description: Blocks BottomSheet when the content
 *   can scroll either UP or DOWN. This provides a smoother experience for group tasks
 *   where subtasks are displayed below.
 *
 * Note: The EditText's touch events are still passed through (returning false), allowing
 * the original scrolling behavior to work alongside the BottomSheet blocking mechanism.
 *
 * @see View.canScrollVertically
 * @see android.view.ViewParent.requestDisallowInterceptTouchEvent
 */
fun EditText.preventBottomSheetInterceptionFully() {
    this.setOnTouchListener { v, event ->
        if (v.canScrollVertically(-1) || v.canScrollVertically(1)) {
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        false
    }
}

/**
 * Prevents the ViewPager2 from scrolling when the user scrolls inside an EditText
 *
 * When the user touches an EditText that has scrollable content, this function:
 * - Blocks the ViewPager2 from intercepting touch events (disables swipe to scroll)
 * - Allows the EditText to handle vertical scrolling naturally
 * - Restores ViewPager2's ability to intercept events when the touch ends
 *
 * Why this is needed:
 * Without this, diagonal swipes on the EditText would be interpreted as
 * ViewPager2 page-swiping gestures instead of vertical text scrolling.
 *
 * The logic:
 * - Note's title and description: Blocks ViewPager2 when the content
 *   can scroll either UP or DOWN.
 *
 * Note: The EditText's touch events are still passed through (returning false), allowing
 * the original scrolling behavior to work alongside the ViewPager2 blocking mechanism.
 *
 * @see View.canScrollVertically
 * @see android.view.ViewParent.requestDisallowInterceptTouchEvent
 */
fun EditText.preventViewPagerInterceptionFully() {
    this.setOnTouchListener { v, event ->
        if (v.canScrollVertically(-1) || v.canScrollVertically(1)) {
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        false
    }
}


/**
 * Dynamically adjusts the maximum number of visible lines for an EditText
 * based on the current screen orientation.
 *
 * In landscape mode, less vertical space is available, so EditText is limited
 * to `landscapeMaxLines` lines to ensure other UI elements have enough room to be used.
 * In portrait mode, more vertical space is available, allowing for up to `portraitMaxLines` lines.
 *
 * **Default values:**
 * - Portrait mode: 4 lines
 * - Landscape mode: 2 lines
 *
 * @param portraitMaxLines Maximum lines in portrait mode (default: 4)
 * @param landscapeMaxLines Maximum lines in landscape mode (default: 2)
 *
 * @see Configuration.ORIENTATION_LANDSCAPE
 * @see EditText.maxLines
 */
fun EditText.adjustMaxLinesBasedOnOrientation(portraitMaxLines: Int = 4, landscapeMaxLines: Int = 2 ){
    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape){
        this.maxLines = landscapeMaxLines
    }
    else {
        this.maxLines = portraitMaxLines
    }
}