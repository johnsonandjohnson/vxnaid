package com.jnj.vaccinetracker.register.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.register.screens.RegisterParticipantAdministeredVaccinesViewModel

class SubstanceItemAdapter(
   private val items: MutableList<RegisterParticipantAdministeredVaccinesViewModel.SubstanceDoseDataModel>
) : RecyclerView.Adapter<SubstanceItemAdapter.SubstanceViewHolder>() {

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_registration_administered_vaccine, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position])
   }

   override fun getItemCount(): Int = items.size

   fun updateList(newSubstances: List<RegisterParticipantAdministeredVaccinesViewModel.SubstanceDoseDataModel>?) {
      items.clear()
      if (newSubstances != null) {
         items.addAll(newSubstances)
      } else {
         items.addAll(emptyList())
      }
      notifyDataSetChanged()
   }

   class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_participantId)
      private val substanceDose: TextView = itemView.findViewById(R.id.textView_dose)

      fun bind(substance: RegisterParticipantAdministeredVaccinesViewModel.SubstanceDoseDataModel) {
         substanceName.text = substance.substance.conceptName
         substanceDose.text = substance.dose.toString()
      }
   }
}