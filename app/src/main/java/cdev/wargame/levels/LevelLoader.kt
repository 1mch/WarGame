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
                        radius = o.optDouble("radius", 13.0).toFloat(),
                        type = o.optString("type", "rock")
                    )
                )
            }
        }

        return LevelData(name, basesList, obsList)
    }

    /**
     * Built-in campaign levels defined in the 350-wide logical coordinate system:
     * Center = (0, 0), Left edge = -175, Right edge = +175.
     */
    fun getBuiltinLevel(levelIndex: Int): LevelData {
        return when (levelIndex) {
            1 -> LevelData(
                name = "Mission 1: First Clash",
                bases = listOf(
                    BaseDef("b1", -110f, 90f, ownerId = 1, initialUnits = 20, type = "large"),
                    BaseDef("b2", 110f, -90f, ownerId = 2, initialUnits = 18, type = "large"),
                    BaseDef("b3", 0f, 0f, ownerId = 0, initialUnits = 10, type = "medium"),
                    BaseDef("b4", -70f, -55f, ownerId = 0, initialUnits = 5, type = "small"),
                    BaseDef("b5", 70f, 55f, ownerId = 0, initialUnits = 5, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -30f, 60f, radius = 12f),
                    ObstacleDef("obs2", 30f, -60f, radius = 12f)
                )
            )
            2 -> LevelData(
                name = "Mission 2: Triangulation",
                bases = listOf(
                    BaseDef("b1", 0f, 115f, ownerId = 1, initialUnits = 25, type = "large"),
                    BaseDef("b2", -115f, -90f, ownerId = 2, initialUnits = 20, type = "medium"),
                    BaseDef("b3", 115f, -90f, ownerId = 3, initialUnits = 20, type = "medium"),
                    BaseDef("b4", 0f, -18f, ownerId = 0, initialUnits = 12, type = "medium"),
                    BaseDef("b5", -80f, 35f, ownerId = 0, initialUnits = 6, type = "small"),
                    BaseDef("b6", 80f, 35f, ownerId = 0, initialUnits = 6, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", 0f, 45f, radius = 12.5f),
                    ObstacleDef("obs2", -52f, -52f, radius = 11f),
                    ObstacleDef("obs3", 52f, -52f, radius = 11f)
                )
            )
            3 -> LevelData(
                name = "Mission 3: Asteroid Chokepoints",
                bases = listOf(
                    BaseDef("b1", -122f, 105f, ownerId = 1, initialUnits = 30, type = "large"),
                    BaseDef("b2", -122f, -105f, ownerId = 1, initialUnits = 15, type = "small"),
                    BaseDef("b3", 122f, 105f, ownerId = 2, initialUnits = 25, type = "large"),
                    BaseDef("b4", 122f, -105f, ownerId = 2, initialUnits = 20, type = "medium"),
                    BaseDef("b5", 0f, 88f, ownerId = 0, initialUnits = 15, type = "medium"),
                    BaseDef("b6", 0f, -88f, ownerId = 0, initialUnits = 15, type = "medium"),
                    BaseDef("b7", 0f, 0f, ownerId = 0, initialUnits = 20, type = "large")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -62f, 35f, radius = 14f),
                    ObstacleDef("obs2", -62f, -35f, radius = 14f),
                    ObstacleDef("obs3", 62f, 35f, radius = 14f),
                    ObstacleDef("obs4", 62f, -35f, radius = 14f)
                )
            )
            4 -> LevelData(
                name = "Mission 4: Quad Siege",
                bases = listOf(
                    BaseDef("b1", -115f, 115f, ownerId = 1, initialUnits = 25, type = "large"),
                    BaseDef("b2", 115f, 115f, ownerId = 2, initialUnits = 22, type = "large"),
                    BaseDef("b3", -115f, -115f, ownerId = 3, initialUnits = 22, type = "large"),
                    BaseDef("b4", 115f, -115f, ownerId = 4, initialUnits = 22, type = "large"),
                    BaseDef("b5", 0f, 0f, ownerId = 0, initialUnits = 30, type = "large"),
                    BaseDef("b6", 0f, 80f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b7", 0f, -80f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b8", -80f, 0f, ownerId = 0, initialUnits = 8, type = "small"),
                    BaseDef("b9", 80f, 0f, ownerId = 0, initialUnits = 8, type = "small")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -50f, 50f, radius = 12.5f),
                    ObstacleDef("obs2", 50f, 50f, radius = 12.5f),
                    ObstacleDef("obs3", -50f, -50f, radius = 12.5f),
                    ObstacleDef("obs4", 50f, -50f, radius = 12.5f)
                )
            )
            else -> LevelData(
                name = "Mission 5: Total Dominion",
                bases = listOf(
                    BaseDef("b1", -122f, 0f, ownerId = 1, initialUnits = 35, type = "large"),
                    BaseDef("b2", 122f, 70f, ownerId = 2, initialUnits = 25, type = "medium"),
                    BaseDef("b3", 122f, -70f, ownerId = 5, initialUnits = 25, type = "medium"),
                    BaseDef("b4", 0f, 105f, ownerId = 3, initialUnits = 20, type = "medium"),
                    BaseDef("b5", 0f, -105f, ownerId = 4, initialUnits = 20, type = "medium"),
                    BaseDef("b6", 0f, 0f, ownerId = 0, initialUnits = 40, type = "large"),
                    BaseDef("b7", -62f, 62f, ownerId = 0, initialUnits = 10, type = "small"),
                    BaseDef("b8", -62f, -62f, ownerId = 0, initialUnits = 10, type = "small"),
                    BaseDef("b9", 62f, 0f, ownerId = 0, initialUnits = 15, type = "medium")
                ),
                obstacles = listOf(
                    ObstacleDef("obs1", -35f, 0f, radius = 14f),
                    ObstacleDef("obs2", 62f, 62f, radius = 12.5f),
                    ObstacleDef("obs3", 62f, -62f, radius = 12.5f)
                )
            )
        }
    }
}
