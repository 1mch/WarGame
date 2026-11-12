package cdev.wargame.core

import android.graphics.Color

data class Faction(
    val id: Int,
    val name: String,
    val primaryColor: Int,
    val glowColor: Int,
    val darkColor: Int
)

object GameConfig {
    // Unit properties
    const val UNIT_RADIUS = 0.02f
    const val BASE_SPEED = 0.35f
    const val SPEED_VARIATION = 0.15f
    const val SPAWN_INTERVAL_MS = 50L
    const val CLASH_DESTROY_CHANCE = 0.90f

    // Steering behavior weights
    const val SEEK_WEIGHT = 5.0f
    const val SEPARATION_WEIGHT = 2.5f
    const val SEPARATION_RADIUS = 1.15f * UNIT_RADIUS
    const val BOUNDARY_MARGIN = 0.08f
    const val BOUNDARY_FORCE = 3.0f
    const val BASE_AVOIDANCE_RADIUS = 0.25f
    const val BASE_REPEL_WEIGHT = 9.0f
    const val BASE_TANGENT_WEIGHT = 1.5f
    const val OBSTACLE_AVOIDANCE_RADIUS = 0.25f
    const val OBSTACLE_REPEL_WEIGHT = 11.0f
    const val OBSTACLE_TANGENT_WEIGHT = 2.2f

    // Visual colors
    val COLOR_BACKGROUND = Color.parseColor("#090D16")
    val COLOR_GRID = Color.argb(8, 255, 255, 255)
    val COLOR_GOLD = Color.parseColor("#FFC629")

    val FACTIONS = mapOf(
        0 to Faction(0, "Neutral", Color.parseColor("#64748B"), Color.parseColor("#94A3B8"), Color.parseColor("#334155")),
        1 to Faction(1, "Player", Color.parseColor("#00D2FF"), Color.parseColor("#38BDF8"), Color.parseColor("#0284C7")),
        2 to Faction(2, "Red", Color.parseColor("#FF3366"), Color.parseColor("#FB7185"), Color.parseColor("#E11D48")),
        3 to Faction(3, "Green", Color.parseColor("#10B981"), Color.parseColor("#34D399"), Color.parseColor("#059669")),
        4 to Faction(4, "Orange", Color.parseColor("#F59E0B"), Color.parseColor("#FBBF24"), Color.parseColor("#D97706")),
        5 to Faction(5, "Purple", Color.parseColor("#A855F7"), Color.parseColor("#C084FC"), Color.parseColor("#7E22CE")),
        6 to Faction(6, "Yellow", Color.parseColor("#EAB308"), Color.parseColor("#FDE047"), Color.parseColor("#CA8A04"))
    )

    fun getFaction(ownerId: Int): Faction {
        return FACTIONS[ownerId] ?: FACTIONS[0]!!
    }
}
