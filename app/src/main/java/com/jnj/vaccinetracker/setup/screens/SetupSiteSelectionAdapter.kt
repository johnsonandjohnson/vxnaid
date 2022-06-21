package com.jnj.vaccinetracker.setup.screens

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.jnj.vaccinetracker.common.ui.model.SiteUiModel

/**
 * @author maartenvangiel
 * @version 1
 */
class SetupSiteSelectionAdapter(context: Context, layoutResource: Int, private val sites: List<SiteUiModel>) : ArrayAdapter<SiteUiModel>(context, layoutResource, sites) {

    private var filteredSites = sites
    private val filter = SiteLocationFilter()

    override fun getCount(): Int {
        return filteredSites.size
    }

    override fun getItem(position: Int): SiteUiModel {
        return filteredSites[position]
    }

    override fun getPosition(item: SiteUiModel?): Int {
        return filteredSites.indexOf(item)
    }

    override fun getFilter(): Filter {
        return filter
    }

    inner class SiteLocationFilter : Filter() {

        private fun List<SiteUiModel>.toFilterResults() = FilterResults().also {
            it.count = size
            it.values = this
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val constraintString = constraint?.toString()
            return if (constraintString.isNullOrEmpty()) {
                sites.toFilterResults()
            } else {
                fun SiteUiModel.matchesConstraints(): Boolean = name.contains(constraintString, ignoreCase = true) || displayName.contains(constraintString, ignoreCase = false)

                sites.map { it to it.matchesConstraints() }
                    .sortedByDescending { (_, isMatch) -> isMatch }  // show matching sites first
                    .map { (item, _) -> item }
                    .toFilterResults()
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val resultSites = results?.values as? List<SiteUiModel>?
            filteredSites = resultSites.orEmpty()
            notifyDataSetChanged()
        }
    }

}