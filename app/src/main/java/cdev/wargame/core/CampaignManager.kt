package cdev.wargame.core

import android.content.Context
import android.content.SharedPreferences

class CampaignManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wargame_campaign", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var unlockedLevel: Int
        get() = prefs.getInt("unlocked_level", 1)
        private set(value) = prefs.edit().putInt("unlocked_level", value).apply()

    var winsCount: Int
        get() = prefs.getInt("wins_count", 0)
        private set(value) = prefs.edit().putInt("wins_count", value).apply()

    val maxCampaignLevels: Int = 5

    fun completeLevel(levelIndex: Int) {
        winsCount++
        if (levelIndex >= unlockedLevel && unlockedLevel < maxCampaignLevels) {
            unlockedLevel = levelIndex + 1
        }
    }

    fun getRankName(): String {
        return when (unlockedLevel) {
            1 -> "Cadet"
            2 -> "Lieutenant"
            3 -> "Commander"
            4 -> "Colonel"
            5 -> "General"
            else -> "Fleet Admiral"
        }
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
    }
}
