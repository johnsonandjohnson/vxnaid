package com.jnj.vaccinetracker.common.ui

import androidx.fragment.app.FragmentTransaction
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection

fun FragmentTransaction.animateNavigationDirection(navigationDirection: NavigationDirection): FragmentTransaction {
    // Set animation based on navigationDirection
    return when (navigationDirection) {
        NavigationDirection.FORWARD -> {
            setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        NavigationDirection.BACKWARD -> {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        NavigationDirection.NONE -> {
            //no-op
            this
        }
    }
}