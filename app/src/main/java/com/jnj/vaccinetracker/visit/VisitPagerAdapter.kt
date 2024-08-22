package com.jnj.vaccinetracker.visit

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visit.screens.VisitVaccinesFragment
import com.jnj.vaccinetracker.visit.screens.VisitCaptureDataFragment

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitPagerAdapter(private val context: Context, fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private companion object {
        private const val PAGE_COUNT = 2
        private const val PAGE_CAPTURE_DATA = 0
        private const val PAGE_VACCINES = 1
    }

    private var visitVaccinesFragment: VisitVaccinesFragment? = null
    private var visitCaptureDataFragment: VisitCaptureDataFragment? = null

    override fun getCount(): Int = PAGE_COUNT

    override fun getItem(position: Int): Fragment {
        return when (position) {
            PAGE_VACCINES -> getVisitVaccinesFragment()
            PAGE_CAPTURE_DATA -> getVisitCaptureDataFragment()
            else -> error("No item exists for position $position")
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            PAGE_VACCINES -> context.getString(R.string.visit_vaccines_tab)
            PAGE_CAPTURE_DATA -> context.getString(R.string.visit_capture_data_tab)
            else -> error("No item exists for position $position")
        }
    }

    private fun getVisitVaccinesFragment(): Fragment {
        return visitVaccinesFragment ?: VisitVaccinesFragment().also { visitVaccinesFragment = it }
    }

    private fun getVisitCaptureDataFragment(): Fragment {
        return visitCaptureDataFragment ?: VisitCaptureDataFragment().also { visitCaptureDataFragment = it }
    }
}