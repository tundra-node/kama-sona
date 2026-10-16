package com.elias.kamasona.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.elias.kamasona.MainActivity
import com.elias.kamasona.R
import com.elias.kamasona.audio.Speaker
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyFragment : Fragment() {

    private lateinit var store: Store

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_daily, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        store = (activity as MainActivity).store

        val today = store.today()
        val pretty = SimpleDateFormat("EEEE, MMMM d", Locale.US).format(Date())
        v.findViewById<TextView>(R.id.dayTitle).text = "tenpo ni · $pretty"

        val wotd = wordOfTheDay(today)
        val wotdGlyph = v.findViewById<TextView>(R.id.wotdGlyph)
        wotdGlyph.text = wotd?.let { Sitelen.glyphText(wotdGlyph, it) } ?: ""
        v.findViewById<TextView>(R.id.wotdWord).text = wotd?.word ?: ""
        v.findViewById<TextView>(R.id.wotdDef).text = wotd?.shortDef ?: ""
        v.findViewById<Button>(R.id.wotdPlay).setOnClickListener {
            wotd?.let { Speaker.say(requireContext(), it.word) }
        }
        wotd?.let {
            requireContext().getSharedPreferences("kama_sona", android.content.Context.MODE_PRIVATE)
                .edit().putString("today_word", it.word).apply()
        }
        v.findViewById<TextView>(R.id.attribution).text = Dict.attribution

        val input = v.findViewById<EditText>(R.id.newSentence)
        v.findViewById<Button>(R.id.addSentence).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            store.addSentence(text)
            input.setText("")
            refresh(v)
        }

        refresh(v)
    }

    override fun onResume() {
        super.onResume()
        view?.let { refresh(it) }
    }

    private fun refresh(v: View) {
        val streak = store.streak()
        v.findViewById<TextView>(R.id.streak).text = when {
            streak == 0 -> "streak: ala — write one sentence to start"
            else -> "streak: $streak tenpo suno"
        }
        val (rev, sent) = store.todayCounts()
        v.findViewById<TextView>(R.id.today).text =
            "today: $sent ${if (sent == 1) "sentence" else "sentences"} · " +
                "$rev card ${if (rev == 1) "review" else "reviews"}"

        val box = v.findViewById<LinearLayout>(R.id.sentences)
        box.removeAllViews()
        val today = store.today()
        for ((id, body) in store.sentencesFor(today)) {
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 10, 0, 10)
            val tv = TextView(requireContext())
            tv.text = "• $body"
            tv.textSize = 15f
            tv.setTextColor(requireContext().getColor(R.color.textPrimary))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tv.layoutParams = lp
            row.addView(tv)
            val del = TextView(requireContext())
            del.text = " ✕"
            del.textSize = 15f
            del.setTextColor(requireContext().getColor(R.color.textSecondary))
            del.setOnClickListener {
                store.deleteSentence(id)
                refresh(v)
            }
            row.addView(del)
            box.addView(row)
        }
    }

    /** Deterministic per-day pick so the word is stable all day. */
    private fun wordOfTheDay(day: String) = run {
        val pool = Dict.words.filter { it.isCore }
        if (pool.isEmpty()) null
        else pool[Math.floorMod(day.hashCode(), pool.size)]
    }
}

/** ISO-ish week key, e.g. 2026-W42. */
fun weekKey(): String {
    val cal = Calendar.getInstance()
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return "${cal.get(Calendar.YEAR)}-W$week"
}
