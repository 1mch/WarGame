package cdev.wargame.levels

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
    val radius: Float = 0.075f,
    val type: String = "rock"
)

data class LevelData(
    val name: String = "Operation",
    val bases: List<BaseDef>,
    val obstacles: List<ObstacleDef> = emptyList()
)
