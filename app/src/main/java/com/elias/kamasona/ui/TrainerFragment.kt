package com.elias.kamasona.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.elias.kamasona.MainActivity
import com.elias.kamasona.R
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store
import com.elias.kamasona.data.Word
import com.elias.kamasona.gloss.Gloss
import kotlin.random.Random

class TrainerFragment : Fragment() {

    private lateinit var store: Store
    private var quizWord: Word? = null
    private var asked = 0
    private var right = 0

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_trainer, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        store = (activity as MainActivity).store
        val input = v.findViewById<EditText>(R.id.sentence)
        val results = v.findViewById<LinearLayout>(R.id.results)

        v.findViewById<Button>(R.id.gloss).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "o sitelen e linja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            results.removeAllViews()
            val r = Gloss.run(text)
            for (c in r.chunks) {
                val row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_chunk, results, false)
                row.findViewById<TextView>(R.id.chunkText).text = c.text
                row.findViewById<TextView>(R.id.chunkNote).text =
                    "${c.role.name.lowercase().replace('_', ' ')} — ${c.note}"
                results.addView(row)
            }
            for (n in r.notes) {
                val tv = TextView(requireContext())
                tv.text = "· $n"
                tv.textSize = 13f
                tv.setTextColor(requireContext().getColor(R.color.textSecondary))
                tv.setPadding(0, 8, 0, 0)
                results.addView(tv)
            }
            if (r.unknown.isNotEmpty()) {
                val tv = TextView(requireContext())
                tv.text = "· not in the dictionary: ${r.unknown.joinToString(", ")}"
                tv.textSize = 13f
                tv.setTextColor(requireContext().getColor(R.color.textSecondary))
                tv.setPadding(0, 8, 0, 0)
                results.addView(tv)
            }
        }

        v.findViewById<Button>(R.id.save).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            store.addSentence(text)
            Toast.makeText(requireContext(), "saved to today's journal", Toast.LENGTH_SHORT).show()
        }

        v.findViewById<Button>(R.id.quizNext).setOnClickListener { nextQuiz(v) }
        nextQuiz(v)
    }

    private fun nextQuiz(v: View) {
        val pool = Dict.words.filter { it.isCore && it.defEn.isNotBlank() }
        if (pool.size < 8) return
        val target = pool[Random.nextInt(pool.size)]
        quizWord = target

        val qView = v.findViewById<TextView>(R.id.quizQuestion)
        // glyph question: only ask it if the bundled font can actually draw the glyph,
        // otherwise the question would just be the word and the answer would be free
        val glyph = if (Random.nextInt(2) == 0) Sitelen.glyphText(qView, target) else ""
        val kind = if (glyph.isNotEmpty()) 0 else 1
        if (kind == 1) qView.typeface = android.graphics.Typeface.DEFAULT
        val question: String
        val correct: String
        if (kind == 0) {
            question = glyph
            correct = "${target.word} — ${target.shortDef}"
        } else {
            question = target.shortDef
            correct = target.word
        }

        val options = ArrayList<String>()
        options.add(correct)
        var guard = 0
        while (options.size < 4 && guard < 200) {
            guard++
            val other = pool[Random.nextInt(pool.size)]
            if (other.word == target.word) continue
            val o = if (kind == 0) "${other.word} — ${other.shortDef}" else other.word
            if (!options.contains(o)) options.add(o)
        }
        options.shuffle(Random(System.currentTimeMillis()))

        qView.text = question
        val box = v.findViewById<LinearLayout>(R.id.quizOptions)
        box.removeAllViews()
        for (o in options) {
            val b = Button(requireContext())
            b.text = o
            b.isAllCaps = false
            b.textSize = 14f
            b.setOnClickListener {
                asked++
                val ok = o == correct
                if (ok) right++
                Toast.makeText(
                    requireContext(),
                    if (ok) "pona!" else "ala — ${target.word} = ${target.shortDef}",
                    Toast.LENGTH_SHORT
                ).show()
                for (i in 0 until box.childCount) box.getChildAt(i).isEnabled = false
                v.findViewById<TextView>(R.id.quizScore).text =
                    "$right pona / $asked · ${if (asked > 0) (100 * right / asked) else 0}%"
            }
            box.addView(b)
        }
    }
}
