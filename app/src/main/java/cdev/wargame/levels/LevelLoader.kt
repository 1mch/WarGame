package cdev.wargame.levels

import android.content.Context
import org.json.JSONObject

object LevelLoader {

    fun loadLevel(context: Context?, levelIndex: Int): LevelData {
        if (context != null) {
            val fileName = "levels/level$levelIndex.json"
            try {
                context.assets.open(fileName).use { stream ->
                    val jsonStr = stream.bufferedReader().use { it.readText() }
                    return parseJson(jsonStr, "Mission $levelIndex")
                }
            } catch (_: Exception) {
                // Fallback to built-in programmatic level if asset not found
            }
        }
        return getBuiltinLevel(levelIndex)
    }

    fun parseJson(jsonStr: String, defaultName: String = "Operation"): LevelData {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", defaultName)

        val basesList = mutableListOf<BaseDef>()
        val basesArray = root.optJSONArray("bases")
        if (basesArray != null) {
            for (i in 0 until basesArray.length()) {
                val b = basesArray.getJSONObject(i)
                basesList.add(
                    BaseDef(
                        id = b.optString("id", "b$i"),
                        x = b.getDouble("x").toFloat(),
                        y = b.getDouble("y").toFloat(),
                        ownerId = b.getInt("ownerId"),
                        initialUnits = b.getInt("initialUnits"),
                        type = b.optString("type", "medium")
                    )
                )
            }
        }

        val obsList = mutableListOf<ObstacleDef>()
        val obsArray = root.optJSONArray("obstacles")
        if (obsArray != null) {
            for (i in 0 until obsArray.length()) {
                val o = obsArray.getJSONObject(i)
                obsList.add(
                    ObstacleDef(
                        id = o.optString("id", "obs$i"),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        radius = o.optDouble("radius", 0.075).toFloat(),
                        type = o.optString("type", "rock")
                    )
                )
            }
        }

        return LevelData(name, basesList, obsList)
    }

    fun getBuiltinLevel(levelIndex: Int): LevelData {
        return when (levelIndex) {
            1 -> LevelData(
                name = "Mission 1: First Clash",
                bases = listOf(
                    BaseDef("b1", -0.6f, 0.5f, ownerId = 1, initialUnits = 20, type = "large"),
                    BaseDef("b2", 0.6f, -0.5f, ownerId = 2, initialUnits = 18, type = "large"),
                    BaseDef("b3", 0.0f, 0.0f, ownerId = 0, initialUnits = 10, type = "medium"),
                    BaseDef("b4", -0.4f, -0.3f, ownerId = 0, initialUnits = 5, type = "small"),
                    BaseDef("b5", 0.4f, 0.3f, ownerId = 0, initialUnits = 5, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -0.15f, 0.35f, radius = 0.065f),
                    ObstacleDef("obs2", 0.15f, -0.35f, radius = 0.065f)
                )
            )
            2 -> LevelData(
                name = "Mission 2: Triangulation",
                bases = listOf(
                    BaseDef("b1", 0.0f, 0.65f, ownerId = 1, initialUnits = 25, type = "large"),
                    BaseDef("b2", -0.65f, -0.5f, ownerId = 2, initialUnits = 20, type = "medium"),
                    BaseDef("b3", 0.65f, -0.5f, ownerId = 3, initialUnits = 20, type = "medium"),
                    BaseDef("b4", 0.0f, -0.1f, ownerId = 0, initialUnits = 12, type = "medium"),
                    BaseDef("b5", -0.45f, 0.2f, ownerId = 0, initialUnits = 6, type = "small"),
                    BaseDef("b6", 0.45f, 0.2f, ownerId = 0, initialUnits = 6, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", 0.0f, 0.25f, radius = 0.07f),
                    ObstacleDef("obs2", -0.3f, -0.3f, radius = 0.06f),
                    ObstacleDef("obs3", 0.3f, -0.3f, radius = 0.06f)
                )
            )
            3 -> LevelData(
                name = "Mission 3: Asteroid Chokepoints",
                bases = listOf(
                    BaseDef("b1", -0.7f, 0.6f, ownerId = 1, initialUnits = 30, type = "large"),
                    BaseDef("b2", -0.7f, -0.6f, ownerId = 1, initialUnits = 15, type = "small"),
                    BaseDef("b3", 0.7f, 0.6f, ownerId = 2, initialUnits = 25, type = "large"),
                    BaseDef("b4", 0.7f, -0.6f, ownerId = 2, initialUnits = 20, type = "medium"),
                    BaseDef("b5", 0.0f, 0.5f, ownerId = 0, initialUnits = 15, type = "medium"),
                    BaseDef("b6", 0.0f, -0.5f, ownerId = 0, initialUnits = 15, type = "medium"),
                    BaseDef("b7", 0.0f, 0.0f, ownerId = 0, initialUnits = 20, type = "large")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -0.35f, 0.2f, radius = 0.08f),
                    ObstacleDef("obs2", -0.35f, -0.2f, radius = 0.08f),
                    ObstacleDef("obs3", 0.35f, 0.2f, radius = 0.08f),
                    ObstacleDef("obs4", 0.35f, -0.2f, radius = 0.08f)
                )
            )
            4 -> LevelData(
                name = "Mission 4: Quad Siege",
                bases = listOf(
                    BaseDef("b1", -0.65f, 0.65f, ownerId = 1, initialUnits = 25, type = "large"),
                    BaseDef("b2", 0.65f, 0.65f, ownerId = 2, initialUnits = 22, type = "large"),
                    BaseDef("b3", -0.65f, -0.65f, ownerId = 3, initialUnits = 22, type = "large"),
                    BaseDef("b4", 0.65f, -0.65f, ownerId = 4, initialUnits = 22, type = "large"),
                    BaseDef("b5", 0.0f, 0.0f, ownerId = 0, initialUnits = 30, type = "large"),
                    BaseDef("b6", 0.0f, 0.45f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b7", 0.0f, -0.45f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b8", -0.45f, 0.0f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b9", 0.45f, 0.0f, ownerId = 0, initialUnits = 8, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -0.28f, 0.28f, radius = 0.07f),
                    ObstacleDef("obs2", 0.28f, 0.28f, radius = 0.07f),
                    ObstacleDef("obs3", -0.28f, -0.28f, radius = 0.07f),
                    ObstacleDef("obs4", 0.28f, -0.28f, radius = 0.07f)
                )
            )
            else -> LevelData(
                name = "Mission 5: Total Dominion",
                bases = listOf(
                    BaseDef("b1", -0.7f, 0.0f, ownerId = 1, initialUnits = 35, type = "large"),
                    BaseDef("b2", 0.7f, 0.4f, ownerId = 2, initialUnits = 25, type = "medium"),
                    BaseDef("b3", 0.7f, -0.4f, ownerId = 5, initialUnits = 25, type = "medium"),
                    BaseDef("b4", 0.0f, 0.6f, ownerId = 3, initialUnits = 20, type = "medium"),
                    BaseDef("b5", 0.0f, -0.6f, ownerId = 4, initialUnits = 20, type = "medium"),
                    BaseDef("b6", 0.0f, 0.0f, ownerId = 0, initialUnits = 40, type = "large"),
                    BaseDef("b7", -0.35f, 0.35f, ownerId = 0, initialUnits = 10, type = "small"),
                    BaseDef("b8", -0.35f, -0.35f, ownerId = 0, initialUnits = 10, type = "small"),
                    BaseDef("b9", 0.35f, 0.0f, ownerId = 0, initialUnits = 15, type = "medium")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -0.2f, 0.0f, radius = 0.08f),
                    ObstacleDef("obs2", 0.35f, 0.35f, radius = 0.07f),
                    ObstacleDef("obs3", 0.35f, -0.35f, radius = 0.07f)
                )
            )
        }
    }
}
