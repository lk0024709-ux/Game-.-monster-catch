package com.example.game

import android.content.Context
import android.util.Log

/**
 * Safe manager to preload assets and track their status inside the offline game.
 */
object AssetRepository {

    private val loadedAssets = mutableMapOf<String, String>()

    fun preloadAssets(context: Context) {
        try {
            val assets = listOf(
                "maps/world_map.webp",
                "maps/forest.webp",
                "maps/volcano.webp",
                "creatures/fire_fox.webp",
                "creatures/water_dragon.webp"
            )

            assets.forEach {
                loadedAssets[it] = it
            }
            Log.d("ASSETS", "Assets Loaded Successfully")
        } catch (e: Exception) {
            Log.e("ASSETS", "Asset Loading Failed", e)
        }
    }

    fun getAsset(path: String): String {
        return loadedAssets[path] ?: ""
    }

    fun clear() {
        loadedAssets.clear()
    }
}
