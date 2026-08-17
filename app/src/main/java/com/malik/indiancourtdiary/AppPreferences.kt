package com.malik.indiancourtdiary

import android.content.Context

object AppPreferences {
    private const val FILE = "court_diary_settings"

    fun reminderDays(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt("reminder_days", 1)

    fun reminderHour(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt("reminder_hour", 9)

    fun language(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("language", "English") ?: "English"

    fun onboardingComplete(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("onboarding_complete", false)

    fun completeOnboarding(context: Context, language: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("onboarding_complete", true)
            .putString("language", language)
            .apply()
    }

    fun save(context: Context, days: Int, hour: Int, language: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt("reminder_days", days)
            .putInt("reminder_hour", hour)
            .putString("language", language)
            .apply()
    }
}
