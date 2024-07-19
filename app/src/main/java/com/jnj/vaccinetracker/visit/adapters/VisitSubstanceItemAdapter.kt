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

class VisitSubstanceItemAdapter(
   private val items: MutableList<SubstanceDataModel>,
   private val supportFragmentManager: FragmentManager,
   private val context: Context
) : RecyclerView.Adapter<VisitSubstanceItemAdapter.SubstanceViewHolder>() {

   private var selectedSubstances: MutableMap<String, String> = mutableMapOf()

   companion object {
      private const val TAG_VISIT_VACCINE_DIALOG = "visit_vaccine_dialog"
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_visit_vaccine, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position])
   }

   override fun getItemCount(): Int = items.size

   fun updateList(newSubstances: List<SubstanceDataModel>?) {
      items.clear()
      if (newSubstances != null) {
         items.addAll(newSubstances)
      } else {
         items.addAll(emptyList())
      }
      notifyDataSetChanged()
   }

   fun colorItems(selectedItems: MutableMap<String, String>) {
      selectedSubstances = selectedItems
      notifyDataSetChanged()
   }

   inner class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_participantId)
      private val btnBarcode: ImageButton = itemView.findViewById(R.id.imageButton_barcodeScanner)
      private val layout: LinearLayout = itemView.findViewById(R.id.linearLayout_item)

      private val grey = ContextCompat.getColor(context, R.color.greyed_out)
      private val green = ContextCompat.getColor(context, R.color.colorSecondaryLight)

      private fun isSelected(substance: SubstanceDataModel): Boolean {
         return selectedSubstances.containsKey(substance.conceptName)
      }

      fun bind(substance: SubstanceDataModel) {
         substanceName.text = substance.conceptName
         btnBarcode.setOnClickListener {
            DialogVaccineBarcode(substance).show(supportFragmentManager, TAG_VISIT_VACCINE_DIALOG)
         }
         layout.setBackgroundColor(if (isSelected(substance)) green else grey)
      }
   }
}
