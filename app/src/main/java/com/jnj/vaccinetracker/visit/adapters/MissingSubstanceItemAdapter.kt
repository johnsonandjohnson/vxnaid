package com.jnj.vaccinetracker.visit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

class MissingSubstanceItemAdapter(
   private val items: List<String>,
) : RecyclerView.Adapter<MissingSubstanceItemAdapter.SubstanceViewHolder>() {

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_dropdown, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position])
   }

   override fun getItemCount(): Int = items.size

   inner class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_item_dropdown)

      fun bind(substanceLabel: String) {
         substanceName.text = substanceLabel
      }
   }
}
