package com.elias.kamasona.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.elias.kamasona.MainActivity
import com.elias.kamasona.R
import com.elias.kamasona.audio.Speaker
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store
import kotlin.random.Random

class CouplesFragment : Fragment() {

    private lateinit var store: Store
    private val prefs by lazy {
        requireContext().getSharedPreferences("kama_sona", Context.MODE_PRIVATE)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_couples, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        store = (activity as MainActivity).store
        v.findViewById<TextView>(R.id.weekLabel).text = "week ${weekKey()}"
        v.findViewById<Button>(R.id.reshuffle).setOnClickListener {
            prefs.edit().remove("weekly_words").apply()
            render(v)
        }
        render(v)
    }

    private fun fiveWords(): List<String> {
        val week = weekKey()
        val saved = prefs.getString("weekly_words", null)
        // a new week gets a new set of words
        val savedWeek = prefs.getString("weekly_words_week", null)
        if (!saved.isNullOrBlank() && (savedWeek == week || savedWeek == null)) {
            // savedWeek == null: words saved by an older build; keep them for this week
            if (savedWeek == null) prefs.edit().putString("weekly_words_week", week).apply()
            return saved.split(",").filter { it.isNotBlank() }
        }
        val pool = Dict.words.filter { it.isCore && it.category == "core" }
        if (pool.isEmpty()) return emptyList()
        val picked = LinkedHashSet<String>()
        val rnd = Random(System.currentTimeMillis())
        var guard = 0
        while (picked.size < 5 && guard < 500) {
            guard++
            picked.add(pool[rnd.nextInt(pool.size)].word)
        }
        val list = picked.toList()
        prefs.edit()
            .putString("weekly_words", list.joinToString(","))
            .putString("weekly_words_week", week)
            .apply()
        return list
    }

    private fun render(v: View) {
        val words = fiveWords()
        val week = weekKey()
        val marks = store.weeklyMarks(week)
        val rows = v.findViewById<LinearLayout>(R.id.rows)
        rows.removeAllViews()

        var selfDone = 0
        var partnerDone = 0

        for (w in words) {
            val word = Dict.word(w)
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, 14, 0, 14)

            val left = LinearLayout(requireContext())
            left.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            left.layoutParams = lp

            val tv = TextView(requireContext())
            val glyph = word?.let { Sitelen.glyphText(tv, it) } ?: ""
            tv.text = if (glyph.isNotEmpty()) "$glyph  $w" else w
            tv.textSize = 19f
            tv.setTextColor(requireContext().getColor(R.color.accent))
            tv.setOnClickListener { Speaker.say(requireContext(), w) }
            left.addView(tv)

            val def = TextView(requireContext())
            def.text = word?.shortDef ?: ""
            def.textSize = 12f
            def.setTextColor(requireContext().getColor(R.color.textSecondary))
            left.addView(def)

            row.addView(left)

            row.addView(personBox(week, w, "self", marks[w to "self"] == true).also {
                if (marks[w to "self"] == true) selfDone++
            })
            row.addView(personBox(week, w, "partner", marks[w to "partner"] == true).also {
                if (marks[w to "partner"] == true) partnerDone++
            })
            rows.addView(row)
        }

        v.findViewById<TextView>(R.id.progressText).text =
            "mi: $selfDone/${words.size} · jan pona: $partnerDone/${words.size}"
    }

    private fun personBox(week: String, word: String, person: String, checked: Boolean): CheckBox {
        val cb = CheckBox(requireContext())
        val lp = LinearLayout.LayoutParams(dp(70), LinearLayout.LayoutParams.WRAP_CONTENT)
        cb.layoutParams = lp
        cb.isChecked = checked
        cb.gravity = Gravity.CENTER
        cb.setOnCheckedChangeListener { _, isChecked ->
            store.setWeeklyMark(week, word, person, isChecked)
            view?.let { render(it) }
        }
        return cb
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
}
