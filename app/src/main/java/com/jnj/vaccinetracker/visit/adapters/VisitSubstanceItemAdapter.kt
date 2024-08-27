package com.jnj.vaccinetracker.visit.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.DialogVaccineBarcode
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

@RequiresApi(Build.VERSION_CODES.O)
class VisitSubstanceItemAdapter(
   private val items: MutableList<SubstanceDataModel>,
   private val supportFragmentManager: FragmentManager,
   private val context: Context,
   private val viewModel: VisitViewModel,
   private var suggestedSubstances: List<SubstanceDataModel>? = null,
) : RecyclerView.Adapter<VisitSubstanceItemAdapter.SubstanceViewHolder>() {

   private var selectedSubstances: MutableMap<String, Map<String, String>> = mutableMapOf()
   private var isSuggesting: Boolean = true

   companion object {
      private const val TAG_VISIT_VACCINE_DIALOG = "visit_vaccine_dialog"
   }

   fun setSuggestingMode(isSuggesting: Boolean) {
      this.isSuggesting = isSuggesting
      notifyDataSetChanged()
   }

   fun setSuggestedSubstances(substances: List<SubstanceDataModel>) {
      suggestedSubstances = substances
      notifyDataSetChanged()
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_visit_vaccine, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position], viewModel)
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

   fun colorItems(selectedItems: MutableMap<String, Map<String, String>>) {
      selectedSubstances = selectedItems
      notifyDataSetChanged()
   }

   inner class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_substance_name)
      private val btnBarcode: ImageButton = itemView.findViewById(R.id.imageButton_barcodeScanner)
      private val layout: LinearLayout = itemView.findViewById(R.id.linearLayout_item)
      private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

      private val grey = ContextCompat.getColor(context, R.color.greyed_out)
      private val green = ContextCompat.getColor(context, R.color.colorSecondaryLight)
      private val colorAccent = ContextCompat.getColor(context, R.color.colorAccent)

      private fun isBarcode(substance: SubstanceDataModel): Boolean {
         return selectedSubstances.containsKey(substance.conceptName)
      }

      private fun isSuggested(substance: SubstanceDataModel): Boolean {
         return suggestedSubstances?.any { suggestedSubstance ->
            suggestedSubstance.conceptName == substance.conceptName
         } ?: false
      }

      private fun getBarcode(substance: SubstanceDataModel): String? {
         return selectedSubstances[substance.conceptName]?.get(Constants.BARCODE_STR)
      }

      private fun getManufacturer(substance: SubstanceDataModel): String? {
         return selectedSubstances[substance.conceptName]?.get(Constants.MANUFACTURER_NAME_STR)
      }

      fun bind(substance: SubstanceDataModel, viewModel: VisitViewModel) {
         substanceName.text = substance.label
         val barcode = getBarcode(substance)
         val manufacturerName = getManufacturer(substance)
         btnBarcode.setOnClickListener {
            DialogVaccineBarcode(substance, barcode, manufacturerName).show(supportFragmentManager, TAG_VISIT_VACCINE_DIALOG)
         }
         layout.setBackgroundColor(if (isBarcode(substance)) green else grey)
         layout.backgroundTintList = if (isSuggested(substance) && !isBarcode(substance)) ColorStateList.valueOf(colorAccent) else null
         if (isSuggesting) {
            btnRemove.visibility = View.GONE
         } else {
            btnRemove.visibility = View.VISIBLE
            btnRemove.setOnClickListener{
               viewModel.removeFromSelectedSubstances(substance)
            }
         }
      }
   }
}
