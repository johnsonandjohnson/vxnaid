package com.jnj.vaccinetracker.login

import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import com.jnj.vaccinetracker.settings.mock.MockSettingsDialog

class LoginActivityMenuHelper(private val supportFragmentManager: FragmentManager) {
    companion object {
        private const val MENU_ITEM_MOCK = 123
        private const val TAG_MOCK_SETTINGS_DIALOG = "mock_settings"
    }

    fun onCreateOptionsMenu(menu: Menu) {
        menu.add(Menu.NONE, MENU_ITEM_MOCK, Menu.FIRST, "Mock Settings")
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_ITEM_MOCK) {
            MockSettingsDialog().show(supportFragmentManager, TAG_MOCK_SETTINGS_DIALOG)
            return true
        }
        return false
    }
}