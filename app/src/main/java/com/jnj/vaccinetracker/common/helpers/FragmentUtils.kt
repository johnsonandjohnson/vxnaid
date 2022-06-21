package com.jnj.vaccinetracker.common.helpers

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * @author maartenvangiel
 * @version 1
 */
inline fun <reified T> Fragment.findParent(includeSelf: Boolean = true): T? {
    if (includeSelf && this is T) {
        return this
    }

    (activity as? T)?.let { return it }

    var parent = parentFragment
    while (parent != null) {
        if (parent is T) {
            return parent
        }
        parent = parent.parentFragment
    }

    (activity as? AppCompatActivity)?.findChild<T>()?.let { return it }
    findChild<T>()?.let { return it }
    return null
}

inline fun <reified T> AppCompatActivity.findChild(parentFirst: Boolean = false): T? {
    if (parentFirst) {
        (this as? T)?.let { return it }
    }

    val children = supportFragmentManager.fragments
    children.firstOrNull { it is T }?.let { return it as T? }
    children.forEach { fragment ->
        fragment.findChild<T>()?.let { return it }
    }
    if (!parentFirst) {
        (this as? T)?.let { return it }
    }
    return null
}

inline fun <reified T> Fragment.findChild(): T? {
    if (!isAdded) return null
    val children = childFragmentManager.fragments
    return children.firstOrNull { it is T } as T?
}