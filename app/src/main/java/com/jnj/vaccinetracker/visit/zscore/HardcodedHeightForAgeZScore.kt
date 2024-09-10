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

class HardcodedHeightForAgeZScore(
   name: String,
   gender: Gender,
   birthDateText: String,
) : HardcodedZScore(name, gender, birthDateText) {
   private var labelTextView: TextView? = null

   override fun getValue(): String? = height
   override fun isEmpty(): Boolean {
      return height.isNullOrEmpty()
   }

   override fun onEmpty(): () -> Unit {
      return {labelTextView?.error = "Fill the data"}
   }

   override fun onNotEmpty(): () -> Unit {
      return {labelTextView?.error = null}
   }

   override fun setupView(view: View, listener: OtherSubstanceItemAdapter.AddSubstanceValueListener) {
      val context = view.context
      val linearLayout = createLinearLayout(context)

      labelTextView = createLabelTextView(context)
      val heightInputEditText = createHeightInputEditText(context)
      val valueTextView = createValueTextView(context, height)

      heightInputEditText.addTextChangedListener(createTextWatcher(listener, valueTextView))

      linearLayout.apply {
         addView(labelTextView)
         addView(heightInputEditText)
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
         text = AppResources(context).getString(R.string.visit_z_score_height_label)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )
      }
   }

   private fun createHeightInputEditText(context: android.content.Context): EditText {
      val heightPointer = height
      return EditText(context).apply {
         inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
         hint = AppResources(context).getString(R.string.visit_dosing_hint_cm)
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
         )
         setText(heightPointer)
      }
   }

   private fun createValueTextView(context: android.content.Context, height: String?): TextView {
      val textContent = HeightZScoreCalculator(height, gender, birthDateText).calculateZScoreAndRating() ?: ""
      return TextView(context).apply {
         text = textContent.toString()
         layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
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
            height = s?.toString()
            val text = HeightZScoreCalculator(height, gender, birthDateText).calculateZScoreAndRating() ?: ""
            valueTextView.text = text.toString()
            listener.addOtherSubstance(conceptName, getValue().orEmpty())
         }

         override fun afterTextChanged(s: Editable?) {
            // No action needed here
         }
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
