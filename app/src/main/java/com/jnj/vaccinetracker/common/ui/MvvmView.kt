package com.jnj.vaccinetracker.common.ui

import androidx.lifecycle.LifecycleOwner

interface MvvmView {

    /**
     * for fragments [lifecycleOwner] will be the **viewLifecycleOwner** which can be used to observe **MutableLiveData** or use **LifecycleOwner.coroutineScope** to observe Flows
     *
     * You can also observe your view model with the **viewLifecycleOwner** in **onViewCreated**.
     *
     * The advantages of using this callback are more readable code and prevention of using the wrong **lifecycleOwner**
     */
    fun observeViewModel(lifecycleOwner: LifecycleOwner)
}