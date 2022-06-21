package com.jnj.vaccinetracker.common.helpers

import android.text.Editable
import android.text.TextWatcher

/**
 * @author maartenvangiel
 * @version 1
 */
open class TextWatcherAdapter : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Override in subclass
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Override in subclass
    }

    override fun afterTextChanged(s: Editable?) {
        // Override in subclass
    }

}