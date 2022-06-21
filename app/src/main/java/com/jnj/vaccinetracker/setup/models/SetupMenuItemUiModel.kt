package com.jnj.vaccinetracker.setup.models

import android.content.Context
import com.jnj.vaccinetracker.R

data class SetupMenuItemUiModel(val setupMenuItem: SetupMenuItem, val isDone: Boolean, val showCheckmark: Boolean) {

    fun displayName(context: Context): String = when (setupMenuItem) {
        SetupMenuItem.P2P_TRANSFER -> context.getString(R.string.setup_menu_item_p2p_transfer)
        SetupMenuItem.LICENSES -> context.getString(R.string.setup_menu_item_licenses)
        SetupMenuItem.SITE -> context.getString(R.string.setup_menu_item_site_selection)
        SetupMenuItem.PERMISSIONS -> context.getString(R.string.setup_menu_item_permissions)
    }
}