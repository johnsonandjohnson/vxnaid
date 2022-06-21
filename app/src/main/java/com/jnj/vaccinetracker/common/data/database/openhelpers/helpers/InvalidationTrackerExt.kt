package com.jnj.vaccinetracker.common.data.database.openhelpers.helpers

import androidx.room.InvalidationTracker
import com.jnj.vaccinetracker.common.helpers.logInfo
import java.lang.reflect.Method

private const val ON_AUTO_CLOSE_CALLBACK_METHOD = "onAutoCloseCallback"
private const val FIELD_MANUALLY_CLOSED = "mManuallyClosed"
private const val FIELD_AUTO_CLOSER = "mAutoCloser"

/**
 *
 * WARNING: on version updates, make sure [InvalidationTracker] contains a method named [ON_AUTO_CLOSE_CALLBACK_METHOD]
 */
fun InvalidationTracker.onAutoCloseCallbackReflection() {
    val clazz = this::class.java
    val callback: Method = clazz.getDeclaredMethod(ON_AUTO_CLOSE_CALLBACK_METHOD)
    callback.isAccessible = true
    callback.invoke(this)
}

private fun InvalidationTracker.getAutoCloserReflection(): Any? {
    val clazz = this::class.java
    val autoCloserField = clazz.getDeclaredField(FIELD_AUTO_CLOSER)
    autoCloserField.isAccessible = true
    return autoCloserField.get(this)
}

fun InvalidationTracker.isAutoCloserManuallyClosed(): Boolean {
    val autoCloser = requireNotNull(getAutoCloserReflection())
    val autoCloserClass = autoCloser::class.java
    val manuallyClosedField = autoCloserClass.getDeclaredField(FIELD_MANUALLY_CLOSED)
    manuallyClosedField.isAccessible = true
    return manuallyClosedField.getBoolean(autoCloser)
}

/**
 * if you call close on the database then inside AutoCloser, mIsManuallyClosed will be put to true
 * and the db cannot be reopened. So call this method with **false** to avoid this issue.
 */
fun InvalidationTracker.setAutoCloserManuallyClosedReflection(isManuallyClosed: Boolean) {
    logInfo("setAutoCloserManuallyClosedReflection: $isManuallyClosed")
    val autoCloser = requireNotNull(getAutoCloserReflection())
    val autoCloserClass = autoCloser::class.java
    val manuallyClosedField = autoCloserClass.getDeclaredField(FIELD_MANUALLY_CLOSED)
    manuallyClosedField.isAccessible = true
    manuallyClosedField.setBoolean(autoCloser, isManuallyClosed)
}