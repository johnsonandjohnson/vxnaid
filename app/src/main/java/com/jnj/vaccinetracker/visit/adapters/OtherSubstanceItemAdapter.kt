package com.jnj.vaccinetracker.visit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.zscore.HardcodedZScore

class OtherSubstanceItemAdapter(
    private val items: MutableList<OtherSubstanceDataModel>,
    private val listener: AddSubstanceValueListener,
    private val participant: ParticipantSummaryUiModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var otherSubstanceValues: MutableMap<String, String>? = mutableMapOf()
        set(value) {
            field = value
            // Notify only the HardcodedZScoreViewHolder items
            for (i in items.indices) {
                if (getItemViewType(i) == TYPE_HARDCODED_Z_SCORE) {
                    notifyItemChanged(i)
                }
            }
        }

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_RADIO = 1
        const val TYPE_MULTIPLE_RADIO = 2
        const val TYPE_NUMBER = 3
        const val TYPE_NUMBER_DECIMAL = 4
        const val TYPE_HARDCODED_Z_SCORE = 5
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].inputType) {
            "radio" -> TYPE_RADIO
            "multipleRadio" -> TYPE_MULTIPLE_RADIO
            "text", "" -> TYPE_TEXT
            "number" -> TYPE_NUMBER
            "numberDecimal" -> TYPE_NUMBER_DECIMAL
            "hardcodedZScore" -> TYPE_HARDCODED_Z_SCORE
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_RADIO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_radio_input, parent, false)
                RadioViewHolder(view)
            }
            TYPE_MULTIPLE_RADIO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_multiple_radio_input, parent, false)
                MultipleRadioViewHolder(view)
            }
            TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_text_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_NUMBER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_number_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_NUMBER_DECIMAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_number_decimal_input, parent, false)
                TextViewHolder(view)
            }
            TYPE_HARDCODED_Z_SCORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.other_substance_hardcoded_input, parent, false)
                HardcodedZScoreViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TextViewHolder -> holder.bind(items[position])
            is RadioViewHolder -> holder.bind(items[position])
            is MultipleRadioViewHolder -> holder.bind(items[position])
            is HardcodedZScoreViewHolder -> holder.bind(items[position])
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
                TYPE_TEXT, TYPE_NUMBER, TYPE_NUMBER_DECIMAL -> {
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
                TYPE_HARDCODED_Z_SCORE -> {
                    val holder = recyclerView.findViewHolderForAdapterPosition(index) as? HardcodedZScoreViewHolder
                    if (holder?.isEmpty == true) {
                        holder.onEmpty?.invoke()
                        hasEmptyItems = true
                    } else {
                        holder?.onNotEmpty?.invoke()
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
                labelTextView.error = null
            }
        }
    }

    inner class HardcodedZScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frameLayout: FrameLayout = itemView.findViewById(R.id.frameLayout_hardcoded)
        var isEmpty: Boolean = true
        var onEmpty: (() -> Unit)? = null
        var onNotEmpty: (() -> Unit)? = null
        fun bind(item: OtherSubstanceDataModel) {
            frameLayout.removeAllViews()
            val hardcodedClass: HardcodedZScore =
                HardcodedZScore.fromConceptName(item.conceptName, participant.gender, participant.birthDateText)
            hardcodedClass.setArguments(otherSubstanceValues)
            hardcodedClass.setupView(itemView, listener)
            isEmpty = hardcodedClass.isEmpty()
            onEmpty = hardcodedClass.onEmpty()
            onNotEmpty = hardcodedClass.onNotEmpty()
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
                labelTextView.error = null
            }
        }
    }

    inner class MultipleRadioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelTextView: TextView = itemView.findViewById(R.id.label_otherSubstance)
        private val checkboxGroup: LinearLayout = itemView.findViewById(R.id.linearLayout_checkBoxGroup)

        fun bind(item: OtherSubstanceDataModel) {
            val selectedValues = mutableSetOf<String>()
            labelTextView.text = item.label
            checkboxGroup.removeAllViews()

            item.options.forEachIndexed { index, option ->
                val checkBox = CheckBox(itemView.context).apply {
                    text = option
                    id = View.generateViewId()
                    tag = index
                }

                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedValues.add(option)
                    } else {
                        selectedValues.remove(option)
                    }
                    listener.addOtherSubstance(item.conceptName, selectedValues.toString())
                }

                checkboxGroup.addView(checkBox)
            }
        }
    }


    interface AddSubstanceValueListener {
        fun addOtherSubstance(substanceName: String, value: String)
    }
}