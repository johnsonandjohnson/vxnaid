package com.jnj.vaccinetracker.common.helpers

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

@Suppress("NOTHING_TO_INLINE")
inline fun View.hideKeyboard(): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.hideSoftInputFromWindow(windowToken, 0)
}

@Suppress("NOTHING_TO_INLINE")
inline fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

@Suppress("NOTHING_TO_INLINE")
inline fun Activity.hideKeyboard(): Boolean {
    return currentFocus?.hideKeyboard() ?: false
}

@Suppress("NOTHING_TO_INLINE")
inline fun Activity.showKeyboard() {
    currentFocus?.showKeyboard()
}

@Suppress("NOTHING_TO_INLINE")
inline fun Int.dpif(context: Context): Float = context.resources.getDimension(this)