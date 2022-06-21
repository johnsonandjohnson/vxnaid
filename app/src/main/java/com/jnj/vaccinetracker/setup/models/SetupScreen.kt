package com.jnj.vaccinetracker.setup.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Screens within the setup activity flow, in order.
 */

sealed class SetupScreen : Parcelable {
    companion object {
        fun values() = listOf(Intro, Backend, MainMenu.Menu)
    }

    @Parcelize
    object Intro : SetupScreen()

    @Parcelize
    object Backend : SetupScreen()

    sealed class MainMenu : SetupScreen() {
        @Parcelize
        object Menu : MainMenu()

        sealed class Item : MainMenu() {
            @Parcelize
            object Site : Item()

            @Parcelize
            object Licenses : Item()

            @Parcelize
            object Permissions : Item()

            sealed class P2pSync : Item() {
                @Parcelize
                object DeviceTypeChooser : P2pSync()

                @Parcelize
                object Transfer : P2pSync()

                companion object {
                    fun values() = listOf(DeviceTypeChooser, Transfer)
                }
            }
        }
    }


}