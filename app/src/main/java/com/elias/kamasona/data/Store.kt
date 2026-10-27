package com.elias.kamasona.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local progress store: SRS cards, review log, journal sentences, daily activity.
 * One plain SQLite file, no sync, no accounts.
 */
class Store(ctx: Context) : SQLiteOpenHelper(ctx, "kama_sona.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE cards (
                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                 word TEXT NOT NULL,
                 direction TEXT NOT NULL,
                 deck TEXT NOT NULL DEFAULT 'vocab',
                 ease REAL NOT NULL DEFAULT 2.5,
                 interval_days REAL NOT NULL DEFAULT 0,
                 reps INTEGER NOT NULL DEFAULT 0,
                 lapses INTEGER NOT NULL DEFAULT 0,
                 due INTEGER NOT NULL DEFAULT 0,
                 last INTEGER NOT NULL DEFAULT 0,
                 known INTEGER NOT NULL DEFAULT 0,
                 UNIQUE(word, direction)
               )"""
        )
        db.execSQL("CREATE INDEX idx_cards_due ON cards(due)")
        db.execSQL(
            """CREATE TABLE reviews (
                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                 card_id INTEGER NOT NULL,
                 grade INTEGER NOT NULL,
                 ts INTEGER NOT NULL,
                 interval_days REAL NOT NULL
               )"""
        )
        db.execSQL(
            """CREATE TABLE journal (
                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                 day TEXT NOT NULL,
                 body TEXT NOT NULL,
                 ts INTEGER NOT NULL
               )"""
        )
        db.execSQL(
            """CREATE TABLE activity (
                 day TEXT PRIMARY KEY,
                 reviews INTEGER NOT NULL DEFAULT 0,
                 sentences INTEGER NOT NULL DEFAULT 0
               )"""
        )
        db.execSQL(
            """CREATE TABLE weekly (
                 week TEXT NOT NULL,
                 word TEXT NOT NULL,
                 person TEXT NOT NULL,
                 used INTEGER NOT NULL DEFAULT 0,
                 PRIMARY KEY(week, word, person)
               )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) { /* v1 */ }

    // ---------------------------------------------------------------- cards

    /**
     * Add any cards that are not present yet. The inserts use CONFLICT_IGNORE so
     * this is safe to call repeatedly — it tops the deck up rather than resetting it.
     */
    fun seedMissing(words: List<Word>, directions: List<String>, deck: String = "vocab") {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (w in words) {
                for (d in directions) {
                    val cv = ContentValues().apply {
                        put("word", w.word)
                        put("direction", d)
                        put("deck", deck)
                        put("ease", 2.5)
                        put("interval_days", 0.0)
                        put("due", System.currentTimeMillis())
                        put("last", 0)
                        put("reps", 0)
                    }
                    db.insertWithOnConflict("cards", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    data class Card(
        val id: Long, val word: String, val direction: String,
        val ease: Double, val intervalDays: Double, val reps: Int,
        val lapses: Int, val due: Long, val last: Long, val known: Boolean,
    )

    fun dueCards(limit: Int, includeNew: Boolean): List<Card> {
        val now = System.currentTimeMillis()
        val knownClause = if (includeNew) "" else " AND reps > 0"
        return readableDatabase.rawQuery(
            "SELECT id,word,direction,ease,interval_days,reps,lapses,due,last,known " +
                "FROM cards WHERE due <= $now$knownClause ORDER BY RANDOM() LIMIT $limit",
            null
        ).use { c ->
            val out = ArrayList<Card>(c.count)
            while (c.moveToNext()) out.add(c.toCard())
            out
        }
    }

    private fun android.database.Cursor.toCard() = Card(
        getLong(0), getString(1), getString(2), getDouble(3), getDouble(4),
        getInt(5), getInt(6), getLong(7), getLong(8), getInt(9) == 1,
    )

    fun updateCard(card: Card, ease: Double, intervalDays: Double, reps: Int, lapses: Int,
                   due: Long, known: Boolean) {
        writableDatabase.execSQL(
            "UPDATE cards SET ease=?, interval_days=?, reps=?, lapses=?, due=?, last=?, known=? WHERE id=?",
            arrayOf(ease, intervalDays, reps, lapses, due, System.currentTimeMillis(),
                if (known) 1 else 0, card.id)
        )
    }

    fun logReview(cardId: Long, grade: Int, intervalDays: Double) {
        writableDatabase.execSQL(
            "INSERT INTO reviews(card_id,grade,ts,interval_days) VALUES(?,?,?,?)",
            arrayOf(cardId, grade, System.currentTimeMillis(), intervalDays)
        )
        bumpActivity(reviews = 1)
    }

    fun stats(): Stats {
        val db = readableDatabase
        fun one(sql: String): Int = db.rawQuery(sql, null).use { it.moveToFirst(); it.getInt(0) }
        val total = one("SELECT COUNT(*) FROM cards")
        val new = one("SELECT COUNT(*) FROM cards WHERE reps = 0")
        val learned = one("SELECT COUNT(*) FROM cards WHERE reps > 0")
        val known = one("SELECT COUNT(*) FROM cards WHERE known = 1")
        val now = System.currentTimeMillis()
        val due = one("SELECT COUNT(*) FROM cards WHERE due <= $now AND reps > 0")
        return Stats(total, new, learned, known, due)
    }

    data class Stats(val total: Int, val newCards: Int, val learned: Int, val known: Int, val due: Int)

    /** Consecutive days ending today (or yesterday) with any activity. */
    fun streak(): Int {
        val days = readableDatabase.rawQuery(
            "SELECT day FROM activity WHERE reviews > 0 OR sentences > 0 ORDER BY day DESC LIMIT 400", null
        ).use { c ->
            val out = ArrayList<String>(c.count)
            while (c.moveToNext()) out.add(c.getString(0))
            out
        }
        return streakFrom(days, java.util.Calendar.getInstance())
    }

    companion object {
        /**
         * Length of the run of consecutive days in [daysDesc] (yyyy-MM-dd, newest first)
         * that ends on [now]'s date or the day before.
         */
        fun streakFrom(daysDesc: List<String>, now: java.util.Calendar): Int {
            if (daysDesc.isEmpty()) return 0
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            fmt.timeZone = now.timeZone
            var streak = 0
            val cursor = now.clone() as java.util.Calendar
            if (daysDesc[0] != fmt.format(cursor.time)) {
                cursor.add(java.util.Calendar.DAY_OF_YEAR, -1)
                if (daysDesc[0] != fmt.format(cursor.time)) return 0
            }
            for (d in daysDesc) {
                if (d == fmt.format(cursor.time)) {
                    streak++
                    cursor.add(java.util.Calendar.DAY_OF_YEAR, -1)
                } else break
            }
            return streak
        }
    }

    fun todayCounts(): Pair<Int, Int> {
        val day = today()
        return readableDatabase.rawQuery(
            "SELECT reviews, sentences FROM activity WHERE day = ?", arrayOf(day)
        ).use { c ->
            if (c.moveToFirst()) c.getInt(0) to c.getInt(1) else 0 to 0
        }
    }

    private fun bumpActivity(reviews: Int = 0, sentences: Int = 0) {
        // NOTE: Android 7.0 ships SQLite 3.9 — no UPSERT (needs 3.24). Two statements instead.
        val day = today()
        writableDatabase.execSQL(
            "INSERT OR IGNORE INTO activity(day,reviews,sentences) VALUES(?,0,0)", arrayOf(day)
        )
        writableDatabase.execSQL(
            "UPDATE activity SET reviews = reviews + ?, sentences = sentences + ? WHERE day = ?",
            arrayOf(reviews, sentences, day)
        )
    }

    // -------------------------------------------------------------- journal

    fun addSentence(body: String) {
        writableDatabase.execSQL(
            "INSERT INTO journal(day,body,ts) VALUES(?,?,?)",
            arrayOf(today(), body.trim(), System.currentTimeMillis())
        )
        bumpActivity(sentences = 1)
    }

    fun sentencesFor(day: String): List<Pair<Long, String>> =
        readableDatabase.rawQuery(
            "SELECT id, body FROM journal WHERE day = ? ORDER BY ts ASC", arrayOf(day)
        ).use { c ->
            val out = ArrayList<Pair<Long, String>>()
            while (c.moveToNext()) out.add(c.getLong(0) to c.getString(1))
            out
        }

    fun deleteSentence(id: Long) {
        writableDatabase.execSQL("DELETE FROM journal WHERE id = ?", arrayOf(id))
    }

    fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())

    // --------------------------------------------------------------- weekly

    fun weeklyMarks(week: String): Map<Pair<String, String>, Boolean> {
        val out = HashMap<Pair<String, String>, Boolean>()
        readableDatabase.rawQuery(
            "SELECT word, person, used FROM weekly WHERE week = ?", arrayOf(week)
        ).use { c ->
            while (c.moveToNext()) out[c.getString(0) to c.getString(1)] = c.getInt(2) == 1
        }
        return out
    }

    fun setWeeklyMark(week: String, word: String, person: String, used: Boolean) {
        // INSERT OR REPLACE predates UPSERT and works on SQLite 3.9.
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO weekly(week,word,person,used) VALUES(?,?,?,?)",
            arrayOf(week, word, person, if (used) 1 else 0)
        )
    }
}
