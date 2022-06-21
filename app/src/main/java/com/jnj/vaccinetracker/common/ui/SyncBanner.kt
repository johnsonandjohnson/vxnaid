package com.jnj.vaccinetracker.common.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.databinding.BannerSyncBinding
import com.jnj.vaccinetracker.sync.domain.entities.SyncErrorOverview
import com.jnj.vaccinetracker.sync.domain.entities.SyncState


typealias VoidCallback = () -> Unit

class SyncBanner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = BannerSyncBinding.inflate(LayoutInflater.from(context), this, true)
    var onErrorClick: VoidCallback? = null

    var state: SyncState = SyncState.Idle
        set(value) {
            field = value
            when (value) {
                SyncState.OnlineInSync, is SyncState.Idle -> {
                    // We simply don't show any banner when online and in sync
                    visibility = View.GONE
                }
                is SyncState.SyncComplete -> {
                    setLayoutParams(
                        iconVisibility = View.VISIBLE,
                        labelText = R.string.sync_status_complete,
                        labelVararg = value.lastSyncDate.format(),
                        labelColor = R.color.colorTextOnPrimary,
                        iconResource = R.drawable.ic_checkmark_success,
                        bannerColor = R.color.sync_complete)
                }
                SyncState.OnlineSyncing ->
                    setLayoutParams(
                        iconVisibility = View.VISIBLE,
                        labelText = R.string.sync_status_syncing,
                        labelColor = R.color.colorTextOnLight,
                        iconResource = R.drawable.ic_sync,
                        bannerColor = R.color.sync_in_progress)
                is SyncState.Offline ->
                    setLayoutParams(
                        iconVisibility = View.GONE,
                        labelText = R.string.sync_status_offline,
                        labelVararg = value.lastSyncDate.format(),
                        labelColor = R.color.colorTextOnPrimary,
                        bannerColor = R.color.sync_offline)
                is SyncState.OfflineOutOfSync ->
                    setLayoutParams(
                        iconVisibility = View.VISIBLE,
                        labelText = R.string.sync_status_offline,
                        labelVararg = value.lastSyncDate?.format() ?: resources.getString(R.string.general_label_na),
                        labelColor = R.color.colorTextOnPrimary,
                        iconResource = R.drawable.ic_checkmark_alert,
                        bannerColor = R.color.sync_offline_out_of_sync)
                is SyncState.SyncError ->
                    setLayoutParams(
                        iconVisibility = View.VISIBLE,
                        labelText = R.string.sync_status_sync_error,
                        labelSuffix = if (value.isInProgress) "..." else "",
                        labelColor = R.color.colorTextOnPrimary,
                        iconResource = R.drawable.ic_checkmark_alert,
                        bannerColor = R.color.sync_error,
                        errorCount = value.numberOfErrors,
                        onClick = {
                            onErrorClick?.invoke() ?: run {
                                logError("onErrorClick not set in SyncBanner")
                            }
                        })
            }.let { }
        }

    private fun getSelectableItemBackground(): Drawable? {
        val a = context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        return try {
            a.getDrawable(0)
        } finally {
            a.recycle()
        }
    }

    private fun setLayoutParams(
        iconVisibility: Int,
        labelText: Int,
        labelSuffix: String = "",
        labelColor: Int,
        labelVararg: String? = null,
        iconResource: Int? = null,
        bannerColor: Int,
        errorCount: Long = 0L,
        onClick: VoidCallback? = null,
    ) {
        visibility = View.VISIBLE
        binding.labelTextView.text = context.getString(labelText, labelVararg) + labelSuffix
        binding.labelTextView.setTextColor(ContextCompat.getColor(context, labelColor))
        binding.labelIconView.visibility = iconVisibility

        binding.errorNumber.text = errorCount.toString()
        if (errorCount != 0L) {
            binding.errorNumber.visibility = View.VISIBLE
        } else {
            binding.errorNumber.visibility = View.GONE

        }
        if (iconResource != null) {
            binding.labelIconView.setImageResource(iconResource)
        }
        binding.syncBannerLayout.let { v ->
            v.isClickable = onClick != null
            v.isFocusable = onClick != null
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                v.foreground = if (onClick != null) getSelectableItemBackground() else null
            }
            if (onClick != null)
                v.setOnClickListener {
                    onClick()
                }
            else
                v.setOnClickListener(null)
        }
        DrawableCompat.setTint(binding.labelIconView.drawable, ContextCompat.getColor(context, labelColor))
        setBackgroundColor(ContextCompat.getColor(context, bannerColor))
    }
}