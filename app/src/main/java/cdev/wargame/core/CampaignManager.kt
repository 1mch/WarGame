package cdev.wargame.core

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import cdev.wargame.R
import java.text.NumberFormat
import java.util.Locale

class CampaignManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wargame_campaign", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var unlockedLevel: Int
        get() = prefs.getInt("unlocked_level", 1)
        private set(value) = prefs.edit().putInt("unlocked_level", value).apply()

    var playerLevel: Int
        get() = prefs.getInt("player_level", 12)
        set(value) = prefs.edit().putInt("player_level", value).apply()

    var sectorsConquered: Int
        get() = prefs.getInt("sectors_conquered", 12)
        set(value) = prefs.edit().putInt("sectors_conquered", value).apply()

    var winsCount: Int
        get() = prefs.getInt("wins_count", 12)
        private set(value) = prefs.edit().putInt("wins_count", value).apply()

    var totalMatches: Int
        get() = prefs.getInt("total_matches", 14)
        private set(value) = prefs.edit().putInt("total_matches", value).apply()

    var winStreak: Int
        get() = prefs.getInt("win_streak", 4)
        private set(value) = prefs.edit().putInt("win_streak", value).apply()

    var credits: Int
        get() = prefs.getInt("credits", 2450)
        set(value) = prefs.edit().putInt("credits", value).apply()

    var gems: Int
        get() = prefs.getInt("gems", 48)
        set(value) = prefs.edit().putInt("gems", value).apply()

    var lastSector: Int
        get() = prefs.getInt("last_sector", 7)
        set(value) = prefs.edit().putInt("last_sector", value).apply()

    var lastScore: Int
        get() = prefs.getInt("last_score", 4850)
        set(value) = prefs.edit().putInt("last_score", value).apply()

    var lastCreditsReward: Int
        get() = prefs.getInt("last_credits_reward", 150)
        set(value) = prefs.edit().putInt("last_credits_reward", value).apply()

    var lastGemsReward: Int
        get() = prefs.getInt("last_gems_reward", 6)
        set(value) = prefs.edit().putInt("last_gems_reward", value).apply()

    var language: String
        get() = prefs.getString("language", Locale.getDefault().language.let { if (it == "sk") "sk" else "en" }) ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    // Tech upgrades (levels 1..10)
    var techSpeedLevel: Int
        get() = prefs.getInt("tech_speed", 2)
        set(value) = prefs.edit().putInt("tech_speed", value).apply()

    var techRegenLevel: Int
        get() = prefs.getInt("tech_regen", 3)
        set(value) = prefs.edit().putInt("tech_regen", value).apply()

    var techShieldLevel: Int
        get() = prefs.getInt("tech_shield", 1)
        set(value) = prefs.edit().putInt("tech_shield", value).apply()

    var techCapacityLevel: Int
        get() = prefs.getInt("tech_capacity", 2)
        set(value) = prefs.edit().putInt("tech_capacity", value).apply()

    var selectedSkinIndex: Int
        get() = prefs.getInt("selected_skin", 0)
        set(value) = prefs.edit().putInt("selected_skin", value).apply()

    val maxCampaignLevels: Int = 10

    fun completeLevel(levelIndex: Int) {
        winsCount++
        totalMatches++
        winStreak++
        sectorsConquered++
        playerLevel++

        val earnedCredits = 100 + levelIndex * 50
        val earnedGems = 4 + levelIndex * 2
        val earnedScore = 3500 + levelIndex * 650

        credits += earnedCredits
        gems += earnedGems
        lastSector = levelIndex
        lastScore = earnedScore
        lastCreditsReward = earnedCredits
        lastGemsReward = earnedGems

        if (levelIndex >= unlockedLevel && unlockedLevel < maxCampaignLevels) {
            unlockedLevel = levelIndex + 1
        }
    }

    fun recordDefeat(levelIndex: Int) {
        totalMatches++
        winStreak = 0
        lastSector = levelIndex
        lastScore = 850
        lastCreditsReward = 25
        lastGemsReward = 0
        credits += 25
    }

    fun getWinRatePercent(): Float {
        if (totalMatches <= 0) return 0f
        return (winsCount.toFloat() / totalMatches.toFloat()) * 100f
    }

    fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    @StringRes
    fun getRankResId(): Int {
        return when {
            playerLevel < 3 -> R.string.rank_cadet
            playerLevel < 6 -> R.string.rank_lieutenant
            playerLevel < 15 -> R.string.rank_commander
            playerLevel < 25 -> R.string.rank_colonel
            playerLevel < 40 -> R.string.rank_general
            else -> R.string.rank_fleet_admiral
        }
    }

    fun resetProgress() {
        val currentLang = language
        prefs.edit().clear().apply()
        language = currentLang
        playerLevel = 1
        sectorsConquered = 0
        winsCount = 0
        totalMatches = 0
        winStreak = 0
        credits = 500
        gems = 10
        lastSector = 1
        lastScore = 0
        lastCreditsReward = 0
        lastGemsReward = 0
        techSpeedLevel = 1
        techRegenLevel = 1
        techShieldLevel = 1
        techCapacityLevel = 1
        selectedSkinIndex = 0
    }
}
