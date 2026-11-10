package com.webpoema.app

import android.content.Context
import android.content.res.AssetManager
import org.json.JSONArray

data class Poem(val title: String, val author: String, val text: String)

object PoemLoader {

    private var poems: List<Poem>? = null

    fun getRandomPoem(context: Context): Poem {
        poems?.let { return it.random() }

        val poems = loadPoems(context)
        this.poems = poems
        return poems.random()
    }

    fun getAllPoems(context: Context): List<Poem> {
        poems?.let { return it }
        val poems = loadPoems(context)
        this.poems = poems
        return poems
    }

    private fun loadPoems(context: Context): List<Poem> {
        val json = context.assets.open("poems.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val result = mutableListOf<Poem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(Poem(
                title = obj.getString("title"),
                author = obj.getString("author"),
                text = obj.getString("text")
            ))
        }

        return result
    }
}
