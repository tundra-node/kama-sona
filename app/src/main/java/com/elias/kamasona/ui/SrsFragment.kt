package com.elias.kamasona.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.elias.kamasona.MainActivity
import com.elias.kamasona.R
import com.elias.kamasona.audio.Speaker
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store
import com.elias.kamasona.srs.Srs

class SrsFragment : Fragment() {

    private lateinit var store: Store
    private var queue = ArrayList<Store.Card>()
    private var pos = 0
    private var reviewed = 0

    private lateinit var statsTv: TextView
    private lateinit var setup: View
    private lateinit var reviewBox: View
    private lateinit var doneBox: View
    private lateinit var promptKind: TextView
    private lateinit var prompt: TextView
    private lateinit var answer: TextView
    private lateinit var reveal: Button
    private lateinit var grades: LinearLayout
    private lateinit var progress: TextView
    private lateinit var nextInfo: TextView
    private lateinit var doneText: TextView

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_srs, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        store = (activity as MainActivity).store
        statsTv = v.findViewById(R.id.stats)
        setup = v.findViewById(R.id.setup)
        reviewBox = v.findViewById(R.id.review)
        doneBox = v.findViewById(R.id.done)
        promptKind = v.findViewById(R.id.promptKind)
        prompt = v.findViewById(R.id.prompt)
        answer = v.findViewById(R.id.answer)
        reveal = v.findViewById(R.id.reveal)
        grades = v.findViewById(R.id.grades)
        progress = v.findViewById(R.id.progress)
        nextInfo = v.findViewById(R.id.nextInfo)
        doneText = v.findViewById(R.id.doneText)

        v.findViewById<Button>(R.id.seed).setOnClickListener {
            val words = Dict.words
            store.seedMissing(words, listOf("tp>en", "en>tp", "glyph>tp"))
            refresh()
        }
        v.findViewById<Button>(R.id.start).setOnClickListener { startSession() }
        reveal.setOnClickListener { showAnswer() }
        v.findViewById<Button>(R.id.gAgain).setOnClickListener { grade(Srs.AGAIN) }
        v.findViewById<Button>(R.id.gHard).setOnClickListener { grade(Srs.HARD) }
        v.findViewById<Button>(R.id.gGood).setOnClickListener { grade(Srs.GOOD) }
        v.findViewById<Button>(R.id.gEasy).setOnClickListener { grade(Srs.EASY) }
        refresh()
    }

    private fun refresh() {
        val st = store.stats()
        statsTv.text = buildString {
            append("cards ${st.total} · new ${st.newCards} · learned ${st.learned} · due ${st.due}")
            val streak = store.streak()
            if (streak > 0) append(" · streak ${streak} d")
        }
        // Only offer seeding while the deck is smaller than the full dictionary.
        // (Before: adding one word from the dictionary hid this forever.)
        val full = Dict.words.size * 3
        val canSeed = st.total < full
        setup.visibility = if (canSeed) View.VISIBLE else View.GONE
        reviewBox.visibility = View.GONE
        doneBox.visibility = View.VISIBLE
        doneText.text = when {
            st.total == 0 -> "no deck yet — build it below"
            st.due + st.newCards == 0 -> "ala. nothing due right now.\npali pona!"
            else -> "${st.due} due · ${st.newCards} new"
        }
    }

    private fun startSession() {
        queue = ArrayList(store.dueCards(limit = 30, includeNew = true))
        pos = 0
        reviewed = 0
        if (queue.isEmpty()) { refresh(); return }
        setup.visibility = View.GONE
        doneBox.visibility = View.GONE
        reviewBox.visibility = View.VISIBLE
        showCard()
    }

    private fun showCard() {
        if (pos >= queue.size) {
            reviewBox.visibility = View.GONE
            doneBox.visibility = View.VISIBLE
            doneText.text = "session done — $reviewed cards"
            refreshStatsOnly()
            return
        }
        val c = queue[pos]
        val w = Dict.word(c.word) ?: run { pos++; showCard(); return }
        progress.text = "${pos + 1}/${queue.size} · $reviewed reviewed"
        answer.visibility = View.INVISIBLE
        grades.visibility = View.GONE
        reveal.visibility = View.VISIBLE
        nextInfo.text = ""
        when (c.direction) {
            "tp>en" -> {
                promptKind.text = "toki pona → English"
                prompt.text = w.word
                answer.text = w.defEn.ifEmpty { w.shortDef }
            }
            "en>tp" -> {
                promptKind.text = "English → toki pona"
                prompt.text = w.shortDef
                answer.text = w.word
            }
            else -> {
                promptKind.text = "sitelen pona → toki pona"
                prompt.text = Sitelen.glyphText(prompt, w).ifEmpty { w.word }
                answer.text = "${w.word} — ${w.shortDef}"
            }
        }
        Speaker.say(requireContext(), w.word)
    }

    private fun showAnswer() {
        answer.visibility = View.VISIBLE
        reveal.visibility = View.GONE
        grades.visibility = View.VISIBLE
        val c = queue[pos]
        // preview each interval so the buttons mean something
        val labels = listOf(Srs.AGAIN, Srs.HARD, Srs.GOOD, Srs.EASY).map {
            Srs.intervalLabel(Srs.schedule(c, it))
        }
        val btns = listOf<Button>(
            requireView().findViewById(R.id.gAgain),
            requireView().findViewById(R.id.gHard),
            requireView().findViewById(R.id.gGood),
            requireView().findViewById(R.id.gEasy),
        )
        val names = listOf("again", "hard", "good", "easy")
        btns.forEachIndexed { i, b -> b.text = "${names[i]}\n${labels[i]}" }
    }

    private fun grade(g: Int) {
        val c = queue[pos]
        val n = Srs.schedule(c, g)
        store.updateCard(c, n.ease, n.intervalDays, n.reps, n.lapses, n.due, n.known)
        store.logReview(c.id, g, n.intervalDays)
        reviewed++
        pos++
        showCard()
    }

    private fun refreshStatsOnly() {
        val st = store.stats()
        statsTv.text = "cards ${st.total} · new ${st.newCards} · learned ${st.learned} · due ${st.due}"
    }
}
