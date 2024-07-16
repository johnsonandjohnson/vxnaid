package com.jnj.vaccinetracker.register.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.register.screens.RegisterParticipantAdministeredVaccinesViewModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel

class SubstanceItemAdapter(
   private val items: MutableList<SubstanceDataModel>,
   private val viewModel: RegisterParticipantAdministeredVaccinesViewModel,
) : RecyclerView.Adapter<SubstanceItemAdapter.SubstanceViewHolder>() {

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_registration_administered_vaccine, parent, false)
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

   inner class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_participantId)
      private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

      fun bind(substance: SubstanceDataModel, viewModel: RegisterParticipantAdministeredVaccinesViewModel) {
         substanceName.text = substance.conceptName

         btnRemove.setOnClickListener {
            viewModel.removeFromSelectedSubstances(substance)
         }
      }
   }
}