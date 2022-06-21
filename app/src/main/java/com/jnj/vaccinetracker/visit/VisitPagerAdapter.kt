package com.jnj.vaccinetracker.visit

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visit.screens.VisitDosingFragment
import com.jnj.vaccinetracker.visit.screens.VisitOtherFragment

/**
 * @author maartenvangiel
 * @version 1
 */
class VisitPagerAdapter(private val context: Context, fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private companion object {
        private const val PAGE_COUNT = 2
        private const val PAGE_DOSING = 0
        private const val PAGE_OTHER = 1
    }

    private var visitDosingFragment: VisitDosingFragment? = null
    private var visitOtherFragment: VisitOtherFragment? = null

    override fun getCount(): Int = PAGE_COUNT

    override fun getItem(position: Int): Fragment {
        return when (position) {
            PAGE_DOSING -> getVisitDosingFragment()
            PAGE_OTHER -> getVisitOtherFragment()
            else -> error("No item exists for position $position")
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            PAGE_DOSING -> context.getString(R.string.visit_dosing_tab_dosing_visit)
            PAGE_OTHER -> context.getString(R.string.visit_dosing_tab_other_visit)
            else -> error("No item exists for position $position")
        }
    }

    private fun getVisitDosingFragment(): Fragment {
        return visitDosingFragment ?: VisitDosingFragment().also { visitDosingFragment = it }
    }

    private fun getVisitOtherFragment(): Fragment {
        return visitOtherFragment ?: VisitOtherFragment().also { visitOtherFragment = it }
    }

}