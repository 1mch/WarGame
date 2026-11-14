package cdev.wargame.levels

import android.content.Context
import org.json.JSONObject

object LevelLoader {

    /**
     * Loads a level exclusively from JSON assets (`assets/levels/level<levelIndex>.json`).
     * Coordinate system:
     *   Center = (0, 0)
     *   X axis = -175 (left) .. +175 (right)
     *   Y axis = -300 (top)  .. +300 (bottom)
     */
    fun loadLevel(context: Context, levelIndex: Int): LevelData {
        val fileName = "levels/level$levelIndex.json"
        context.assets.open(fileName).use { stream ->
            val jsonStr = stream.bufferedReader().use { it.readText() }
            return parseJson(jsonStr, "Mission $levelIndex")
        }
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
}
