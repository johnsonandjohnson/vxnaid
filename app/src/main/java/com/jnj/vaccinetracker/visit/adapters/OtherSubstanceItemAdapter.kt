package com.jnj.vaccinetracker.visit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel

class OtherSubstanceItemAdapter(
    private val items: MutableList<OtherSubstanceDataModel>,
    private val listener: AddSubstanceValueListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_RADIO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].inputType) {
            "radio" -> TYPE_RADIO
            "text", "" -> TYPE_TEXT
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RADIO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_radio_input, parent, false)
                RadioViewHolder(view)
            }
            TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_text_input, parent, false)
                TextViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }

    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TextViewHolder -> holder.bind(items[position])
            is RadioViewHolder -> holder.bind(items[position])
        }
    }

    fun updateItemsList(otherSubstances: List<OtherSubstanceDataModel>?) {
        items.clear()
        if (otherSubstances != null) {
            items.addAll(otherSubstances)
        } else {
            items.addAll(emptyList())
        }
        notifyDataSetChanged()
    }

    fun checkIfAnyItemsEmpty(itemsValues: MutableMap<String, String>?, recyclerView: RecyclerView): Boolean {
        var hasEmptyItems = false
        items.forEachIndexed { index, item ->
            val itemValue = itemsValues?.get(item.conceptName)
            when (getItemViewType(index)) {
                TYPE_TEXT -> {
                    val holder = recyclerView.findViewHolderForAdapterPosition(index) as? TextViewHolder
                    if (itemValue.isNullOrEmpty()) {
                        holder?.inputEditText?.error = "Please fill before submitting"
                        hasEmptyItems = true
                    } else {
                        holder?.inputEditText?.error = null
                    }
                }
                TYPE_RADIO -> {
                    val holder = recyclerView.findViewHolderForAdapterPosition(index) as? RadioViewHolder
                    if (itemValue.isNullOrEmpty()) {
                        holder?.labelTextView?.error = "Please select an option before submitting"
                        hasEmptyItems = true
                    } else {
                        holder?.labelTextView?.error = null
                    }
                }
            }
        }
        return hasEmptyItems
    }

    inner class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        val inputEditText: EditText = itemView.findViewById(R.id.editText_otherSubstance)

        fun bind(item: OtherSubstanceDataModel) {
            labelTextView.text = item.label
            inputEditText.addTextChangedListener { editable ->
                val value = editable.toString()
                listener.addOtherSubstance(item.conceptName, value)
            }
        }
    }

    inner class RadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        private val radioGroup: RadioGroup = itemView.findViewById(R.id.radioGroup_otherSubstance)

        fun bind(item: OtherSubstanceDataModel) {
            labelTextView.text = item.label
            radioGroup.removeAllViews()
            item.options.forEachIndexed { index, option ->
                val radioButton = RadioButton(itemView.context).apply {
                    text = option
                    id = View.generateViewId()
                    tag = index
                }
                radioGroup.addView(radioButton)
            }

            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedRadioButton = radioGroup.findViewById<RadioButton>(checkedId)
                val selectedIndex = selectedRadioButton.tag as Int
                val selectedValue = item.options[selectedIndex]
                listener.addOtherSubstance(item.conceptName, selectedValue)
            }
        }
    }

    interface AddSubstanceValueListener {
        fun addOtherSubstance(substanceName: String, value: String)
    }
}