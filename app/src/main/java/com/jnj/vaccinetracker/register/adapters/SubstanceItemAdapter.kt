package com.jnj.vaccinetracker.register.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.register.dialogs.AlreadyAdministeredVaccineDatePickerDialog
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.register.screens.HistoricalDataForVisitTypeViewModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime

@RequiresApi(Build.VERSION_CODES.O)
class SubstanceItemAdapter(
   private val items: MutableList<SubstanceDataModel>,
   private val viewModel: HistoricalDataForVisitTypeViewModel,
   private val supportFragmentManager: FragmentManager,
) : RecyclerView.Adapter<SubstanceItemAdapter.SubstanceViewHolder>() {

   companion object {
      const val DIALOG_TAG = "alreadyAdministeredVaccineDatePickerDialog"
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_registration_administered_vaccine, parent, false)
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

   inner class SubstanceViewHolder(itemView: View) :
      RecyclerView.ViewHolder(itemView),
      ScheduleVisitDatePickerDialog.OnDateSelectedListener
   {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_participantId)
      private val btnPickDate: Button = itemView.findViewById(R.id.btn_pick_date)
      private val vaccineDateText: TextView = itemView.findViewById(R.id.textView_vaccination_date)
      private var selectedSubstance: SubstanceDataModel? = null

      fun bind(substance: SubstanceDataModel) {
         selectedSubstance = substance
         substanceName.text = substance.label
         val substanceDateStr = substance.obsDate?.format(DateFormat.FORMAT_DATE) ?: ""
         vaccineDateText.text = substanceDateStr
         if (substanceDateStr == "") {
            btnPickDate.setOnClickListener {
               // nice to have calculate
               AlreadyAdministeredVaccineDatePickerDialog(null, this).show(
                  supportFragmentManager,
                  DIALOG_TAG
               )
            }
         }
      }

      override fun onDateSelected(dateTime: DateTime) {
         val dateString = dateTime.format(DateFormat.FORMAT_DATE)
         vaccineDateText.text = dateString
         if (selectedSubstance != null) {
            viewModel.addVaccineDate(selectedSubstance!!.conceptName, dateString)
         }
      }
   }
}