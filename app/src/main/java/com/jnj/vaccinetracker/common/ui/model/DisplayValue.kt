package com.jnj.vaccinetracker.common.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DisplayValue(val value: String, val display: String) : Parcelable