package com.jnj.vaccinetracker.setup.models

import com.jnj.vaccinetracker.setup.models.SetupScreen.MainMenu.Item

enum class SetupMenuItem {
    LICENSES, PERMISSIONS, SITE, P2P_TRANSFER;

    val screen
        get() = when (this) {
            P2P_TRANSFER -> Item.P2pSync.DeviceTypeChooser
            LICENSES -> Item.Licenses
            SITE -> Item.Site
            PERMISSIONS -> Item.Permissions
        }
}