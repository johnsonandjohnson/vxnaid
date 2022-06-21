package com.jnj.vaccinetracker.common.viewmodel

import android.os.Bundle

/**
 * Provides consistent methods to save and restore instance state of a ViewModel. To be called in onSaveInstanceState and onCreate of the activity/fragment.
 */
abstract class ViewModelWithState : ViewModelBase() {

    abstract fun saveInstanceState(outState: Bundle)

    abstract fun restoreInstanceState(savedInstanceState: Bundle)

}