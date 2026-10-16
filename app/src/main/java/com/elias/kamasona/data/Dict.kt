package com.elias.kamasona.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AudioClip(val by: String, val source: String, val file: String)

data class Glyph(
    val id: String,
    val word: String,
    val ucsur: String,
    val ligature: String,
    val primary: Boolean,
    val deprecated: Boolean,
    val origin: String,
) {
    /** The UTF-16 string that nasin-nanpa renders as this glyph. */
    val char: String get() = ucsurToChar(ucsur)
}

data class Word(
    val word: String,
    val book: String,
    val category: String,
    val defEn: String,
    val defTp: String,
    val commentary: String,
    val etymology: String,
    val sourceLanguage: String,
    val seeAlso: List<String>,
    val era: String,
    val glyph: String,
    val ligature: String,
    val glyphId: String,
    val glyphIds: List<String>,
    val audio: List<AudioClip>,
) {
    val glyphChar: String get() = ucsurToChar(glyph)
    val effectiveGlyph: String get() = if (glyphChar.isNotEmpty()) glyphChar else ligature
    val shortDef: String get() = defEn.split(";").firstOrNull()?.trim() ?: defEn
    val isCore: Boolean get() = category == "core" || category == "common"
}

fun ucsurToChar(ucsur: String): String {
    if (ucsur.isBlank()) return ""
    val hex = ucsur.removePrefix("U+")
    return try {
        val cp = hex.toInt(16)
        String(Character.toChars(cp))
    } catch (e: Exception) {
        ""
    }
}

object Dict {
    var words: List<Word> = emptyList()
        private set
    var glyphs: List<Glyph> = emptyList()
        private set
    var attribution: String = ""
        private set

    private val byWord = HashMap<String, Word>()
    private val byGlyphId = HashMap<String, Glyph>()

    var loaded = false
        private set

    fun load(ctx: Context) {
        if (loaded) return
        val text = ctx.assets.open("dictionary.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        attribution = root.optString("attribution", "")

        val ws = ArrayList<Word>()
        val wArr = root.getJSONArray("words")
        for (i in 0 until wArr.length()) {
            val o = wArr.getJSONObject(i)
            val clips = ArrayList<AudioClip>()
            val aArr = o.optJSONArray("audio") ?: JSONArray()
            for (j in 0 until aArr.length()) {
                val a = aArr.getJSONObject(j)
                clips.add(AudioClip(a.optString("by"), a.optString("source"), a.optString("file")))
            }
            ws.add(
                Word(
                    word = o.getString("word"),
                    book = o.optString("book"),
                    category = o.optString("category"),
                    defEn = o.optString("def_en"),
                    defTp = o.optString("def_tp"),
                    commentary = o.optString("commentary"),
                    etymology = o.optString("etymology"),
                    sourceLanguage = o.optString("source_language"),
                    seeAlso = o.optJSONArray("see_also").toStringList(),
                    era = o.optString("era"),
                    glyph = o.optString("glyph"),
                    ligature = o.optString("ligature"),
                    glyphId = o.optString("glyph_id"),
                    glyphIds = o.optJSONArray("glyphs").toStringList(),
                    audio = clips,
                )
            )
        }
        words = ws
        byWord.clear(); ws.forEach { byWord[it.word] = it }

        val gs = ArrayList<Glyph>()
        val gArr = root.optJSONArray("glyphs") ?: JSONArray()
        for (i in 0 until gArr.length()) {
            val o = gArr.getJSONObject(i)
            gs.add(
                Glyph(
                    id = o.getString("id"),
                    word = o.optString("word"),
                    ucsur = o.optString("ucsur"),
                    ligature = o.optString("ligature"),
                    primary = o.optBoolean("primary"),
                    deprecated = o.optBoolean("deprecated"),
                    origin = o.optString("origin"),
                )
            )
        }
        glyphs = gs
        byGlyphId.clear(); gs.forEach { byGlyphId[it.id] = it }
        loaded = true
    }

    fun word(w: String): Word? = byWord[w]
    fun glyph(id: String): Glyph? = byGlyphId[id]

    fun search(query: String, category: String? = null, limit: Int = 200): List<Word> {
        val q = query.trim().lowercase()
        val pool = if (category == null) words else words.filter { it.category == category }
        if (q.isEmpty()) return pool.take(limit)
        return pool.asSequence()
            .map { w -> w to score(w, q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Word, Int>> { it.second }.thenBy { it.first.word })
            .map { it.first }
            .take(limit)
            .toList()
    }

    private fun score(w: Word, q: String): Int {
        val word = w.word.lowercase()
        return when {
            word == q -> 1000
            word.startsWith(q) -> 700 - word.length
            word.contains(q) -> 500 - word.length
            w.defEn.lowercase().startsWith(q) -> 300
            w.defEn.lowercase().contains(q) -> 200
            w.seeAlso.any { it.lowercase().startsWith(q) } -> 100
            else -> 0
        }
    }

    /** Primary glyph variants for a word, primary first. */
    fun variants(word: String): List<Glyph> {
        val ids = byWord[word]?.glyphIds ?: return emptyList()
        return ids.mapNotNull { byGlyphId[it] }.sortedByDescending { it.primary }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>(length())
        for (i in 0 until length()) out.add(optString(i))
        return out
    }
}
