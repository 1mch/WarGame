package cdev.wargame.levels

/**
 * Coordinates (x, y) and radius are in the 350-wide logical coordinate system:
 * - Center of the battlefield is (0, 0)
 * - Horizontal bounds: x in [-175, +175] (left edge = -175, right edge = +175)
 * - Vertical bounds (on 9:16 portrait): y in approx [-310, +310]
 */
data class BaseDef(
    val id: String,
    val x: Float,
    val y: Float,
    val ownerId: Int,
    val initialUnits: Int,
    val type: String = "medium"
)

data class ObstacleDef(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float = 13f,
    val type: String = "rock"
)

data class LevelData(
    val name: String = "Operation",
    val bases: List<BaseDef>,
    val obstacles: List<ObstacleDef> = emptyList()
)
