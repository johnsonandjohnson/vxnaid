package com.jnj.vaccinetracker.participantflow.model

import android.os.Parcelable
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.ui.model.DisplayValue
import kotlinx.parcelize.Parcelize


@Parcelize
data class ParticipantSummaryUiModel(
    val participantUuid: String,
    val participantId: String,
    val gender: Gender,
    val yearOfBirth: String,
    val vaccine: DisplayValue,
    val participantPicture: ParticipantImageUiModel?,
) : Parcelable