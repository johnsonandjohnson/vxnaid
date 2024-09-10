package com.jnj.vaccinetracker.visit.zscore

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.AppResources
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.dpToPx
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter

class HardcodedMuacZScore(
   name: String,
   gender: Gender,
   birthDateText: String,
) : HardcodedZScore(name, gender, birthDateText) {
   private var labelTextView: TextView? = null

   override fun getValue(): String? = muac

   override fun isEmpty(): Boolean {
      return muac.isNullOrEmpty()
   }

   override fun onEmpty(): () -> Unit {
      return {labelTextView?.error = "Fill data"}
   }

   override fun onNotEmpty(): () -> Unit {
      return {labelTextView?.error = null}
   }

   override fun setupView(
      view: View,
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener
   ) {
      val context = view.context
      val linearLayout = createLinearLayout(context)

      labelTextView = createLabelTextView(context)
      val muacInputEditText = createMuacInputEditText(context)
      val valueTextView = createValueTextView(context, muac)

      muacInputEditText.addTextChangedListener(createTextWatcher(listener, valueTextView))

      linearLayout.apply {
         addView(labelTextView)
         addView(muacInputEditText)
         addView(valueTextView)
      }

      addLinearLayoutToViewGroup(view, linearLayout)
   }

   private fun createLinearLayout(context: android.content.Context): LinearLayout {
      return LinearLayout(context).apply {
         orientation = LinearLayout.VERTICAL
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )
         setPadding(16.dpToPx, 16.dpToPx, 16.dpToPx, 16.dpToPx)
      }
   }

   private fun createLabelTextView(context: android.content.Context): TextView {
      return TextView(context).apply {
         text = AppResources(context).getString(R.string.visit_z_score_muac_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         )
      }
   }

   private fun createMuacInputEditText(context: android.content.Context): EditText {
      return EditText(context).apply {
         inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
         hint = AppResources(context).getString(R.string.visit_dosing_hint_cm)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         )
         setText(muac)
      }
   }

   private fun createValueTextView(context: android.content.Context, muac: String?): TextView {
      val calculator = MuacZScoreCalculator(muac, gender, birthDateText)
      val textContent = calculator.calculateZScoreAndRating() ?: ""
      return TextView(context).apply {
         text = textContent.toString()
         setTextColor(calculator.getTextColorBasedOnZsCoreValue())
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
         ).apply {
            gravity = Gravity.CENTER
         }
         gravity = Gravity.CENTER
      }
   }

   private fun createTextWatcher(
      listener: OtherSubstanceItemAdapter.AddSubstanceValueListener,
      valueTextView: TextView
   ): TextWatcher {
      return object : TextWatcher {
         override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No action needed here
         }

         override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            muac = s?.toString()
            updateValueTextView(valueTextView)
            notifyListener(listener)
         }

         override fun afterTextChanged(s: Editable?) {
            // No action needed here
         }
      }
   }

   private fun updateValueTextView(valueTextView: TextView) {
      val calculator = MuacZScoreCalculator(muac, gender, birthDateText)
      val text = calculator.calculateZScoreAndRating() ?: ""
      valueTextView.text = text.toString()
      valueTextView.setTextColor(calculator.getTextColorBasedOnZsCoreValue())
   }

   private fun notifyListener(listener: OtherSubstanceItemAdapter.AddSubstanceValueListener) {
      getValue()?.let {
         listener.addOtherSubstance(conceptName, it)
      }
   }

   private fun addLinearLayoutToViewGroup(view: View, linearLayout: LinearLayout) {
      if (view is ViewGroup) {
         view.addView(linearLayout)
      } else {
         throw IllegalArgumentException("Provided view is not a ViewGroup")
      }
   }
}
