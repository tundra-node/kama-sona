package com.elias.kamasona.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elias.kamasona.R
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Glyph
import com.elias.kamasona.data.Word
import com.elias.kamasona.data.ucsurToChar

class DictionaryFragment : Fragment() {

    private var category: String? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_dictionary, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        val search = v.findViewById<EditText>(R.id.search)
        val list = v.findViewById<RecyclerView>(R.id.list)
        val count = v.findViewById<TextView>(R.id.count)
        val chips = v.findViewById<LinearLayout>(R.id.chips)

        val adapter = WordAdapter { w ->
            startActivity(Intent(requireContext(), WordActivity::class.java).putExtra("word", w.word))
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        // category chips: All, core, common, uncommon, obscure
        val cats = listOf(null to "ale", "core" to "pu", "common" to "ku suli",
            "uncommon" to "ku lili", "obscure" to "nasa")
        for ((value, label) in cats) {
            val tv = TextView(requireContext())
            tv.text = label
            tv.textSize = 13f
            tv.setPadding(28, 14, 28, 14)
            tv.setTextColor(requireContext().getColor(R.color.textPrimary))
            tv.background = requireContext().getDrawable(R.drawable.chip_bg)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.rightMargin = 12
            tv.layoutParams = lp
            tv.setOnClickListener {
                category = value
                refresh(adapter, count)
            }
            chips.addView(tv)
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) = refresh(adapter, count)
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        refresh(adapter, count)
    }

    private fun refresh(adapter: WordAdapter, count: TextView) {
        val q = view?.findViewById<EditText>(R.id.search)?.text?.toString() ?: ""
        val results = Dict.search(q, category)
        adapter.submit(results)
        count.text = "${results.size} nimi · ${Dict.words.size} total · offline linku data"
    }
}

class WordAdapter(private val onClick: (Word) -> Unit) :
    RecyclerView.Adapter<WordAdapter.Holder>() {

    private val items = ArrayList<Word>()

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val glyph: TextView = v.findViewById(R.id.glyph)
        val word: TextView = v.findViewById(R.id.word)
        val def: TextView = v.findViewById(R.id.def)
        val cat: TextView = v.findViewById(R.id.cat)
    }

    fun submit(list: List<Word>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val w = items[position]
        h.glyph.text = Sitelen.glyphText(h.glyph, w)
        h.word.text = w.word
        h.def.text = w.shortDef
        h.cat.text = w.category
        h.itemView.setOnClickListener { onClick(w) }
    }
}

/** Chip background used by the dictionary filter row. */
fun chipLabel(g: Glyph): String = ucsurToChar(g.ucsur)
