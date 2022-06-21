package com.jnj.vaccinetracker.common.ui

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject

/**
 * @author maartenvangiel
 * @version 1
 */
abstract class BaseDialogFragment : DaggerAppCompatDialogFragment(), ResourcesWrapper, MvvmView, UiFlowExt {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected inline val resourcesWrapper: ResourcesWrapper
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startObservingWhenStarted()
    }

    private fun startObservingWhenStarted() {
        viewLifecycleOwnerLiveData.observe(
            this, { lifecycleOwner ->
                if (lifecycleOwner != null)
                    observeViewModel(lifecycleOwner)
            }
        )
    }

    override fun getInt(resId: Int): Int {
        return resources.getInteger(resId)
    }

    override fun getColor(resId: Int): Int {
        return ContextCompat.getColor(requireContext(), resId)
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        //make this function optional
    }
}
