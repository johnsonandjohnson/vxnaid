package com.jnj.vaccinetracker.common.helpers

import android.widget.EditText


fun EditText.setTextKeepSelection(text: CharSequence) {
    val selection = selectionStart
    setText(text)
    setSelection(selection.coerceAtMost(length()))
}