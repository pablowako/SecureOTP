package com.otpextractor.secureotp.utils

import android.content.Context

enum class AppFilterState {
    DEFAULT, BLACKLISTED, WHITELISTED
}

class AppFilterRepository(context: Context) {
    private val prefs = context.getSharedPreferences(
        "app_filters", Context.MODE_PRIVATE
    )

    fun getState(packageName: String): AppFilterState {
        val value = prefs.getString(packageName, null)
        return value?.let { AppFilterState.valueOf(it) }
            ?: AppFilterState.DEFAULT
    }

    fun setState(packageName: String, state: AppFilterState) {
        if (state == AppFilterState.DEFAULT) {
            prefs.edit().remove(packageName).apply()
        } else {
            prefs.edit().putString(packageName, state.name).apply()
        }
    }

    fun snapshot(packageNames: List<String>): Map<String, AppFilterState> {
        return packageNames.associateWith { getState(it) }
    }

    fun restore(snapshot: Map<String, AppFilterState>) {
        val editor = prefs.edit()
        for ((pkg, state) in snapshot) {
            if (state == AppFilterState.DEFAULT) {
                editor.remove(pkg)
            } else {
                editor.putString(pkg, state.name)
            }
        }
        editor.apply()
    }

    fun setAll(packageNames: List<String>, state: AppFilterState) {
        val editor = prefs.edit()
        for (pkg in packageNames) {
            if (state == AppFilterState.DEFAULT) {
                editor.remove(pkg)
            } else {
                editor.putString(pkg, state.name)
            }
        }
        editor.apply()
    }
}
