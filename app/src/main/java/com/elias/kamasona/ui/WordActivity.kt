package com.elias.kamasona.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elias.kamasona.R
import com.elias.kamasona.audio.Speaker
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store

class WordActivity : AppCompatActivity() {

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word)
        store = Store(applicationContext)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val name = intent.getStringExtra("word") ?: return finish()
        // The process may have been killed while this screen was in the back stack.
        Dict.load(applicationContext)
        val w = Dict.word(name)
        if (w == null) {
            Toast.makeText(this, "unknown word", Toast.LENGTH_SHORT).show()
            return finish()
        }

        title = w.word
        val glyphTv = findViewById<TextView>(R.id.glyph)
        glyphTv.text = Sitelen.glyphText(glyphTv, w)
        findViewById<TextView>(R.id.word).text = w.word
        findViewById<TextView>(R.id.meta).text = buildString {
            if (w.category.isNotEmpty()) append(w.category)
            if (w.book.isNotEmpty()) append(" · ${w.book}")
            if (w.era.isNotEmpty()) append(" · ${w.era}")
            if (w.sourceLanguage.isNotEmpty()) append(" · from ${w.sourceLanguage}")
            val voice = w.audio.firstOrNull { it.source == "kalaasi2023" }?.by
            if (!voice.isNullOrEmpty()) append(" · kalama: $voice")
        }
        findViewById<TextView>(R.id.def).text = w.defEn.ifEmpty { "(no English definition)" }

        val variants = findViewById<TextView>(R.id.variants)
        val vs = Dict.variants(w.word)
        variants.text = if (vs.isEmpty()) "—" else vs.joinToString("  ") {
            val c = it.char
            if (Sitelen.canDraw(variants, c)) c
            else if (Sitelen.canDraw(variants, it.ligature)) it.ligature
            else "□"
        }
        variants.setOnClickListener {
            val text = if (vs.isEmpty()) w.word else vs.first().char.ifEmpty { w.word }
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("sitelen pona", text))
            Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show()
        }

        val comm = findViewById<TextView>(R.id.commentary)
        comm.text = if (w.commentary.isBlank()) "" else "jan sona: ${w.commentary}"
        val etym = findViewById<TextView>(R.id.etymology)
        etym.text = if (w.etymology.isBlank()) "" else "tan: ${w.etymology}"

        val seeAlso = findViewById<LinearLayout>(R.id.seeAlso)
        for (s in w.seeAlso) {
            val tv = TextView(this)
            tv.text = s
            tv.setPadding(28, 14, 28, 14)
            tv.setTextColor(getColor(R.color.textPrimary))
            tv.background = getDrawable(R.drawable.chip_bg)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.rightMargin = 10
            tv.layoutParams = lp
            tv.setOnClickListener {
                startActivity(android.content.Intent(this, WordActivity::class.java)
                    .putExtra("word", s))
                finish()
            }
            seeAlso.addView(tv)
        }

        findViewById<Button>(R.id.play).setOnClickListener { Speaker.say(this, w.word) }
        if (!Speaker.hasClip(this, w.word)) {
            findViewById<Button>(R.id.play).text = "kalama (tts)"
        }

        val addBtn = findViewById<Button>(R.id.addSrs)
        val state = findViewById<TextView>(R.id.srsState)
        addBtn.setOnClickListener {
            store.writableDatabase.execSQL(
                "INSERT OR IGNORE INTO cards(word,direction,due) VALUES(?,'tp>en',?)," +
                    "(?,'en>tp',?)," +
                    "(?,'glyph>tp',?)",
                arrayOf(w.word, System.currentTimeMillis(), w.word, System.currentTimeMillis(),
                    w.word, System.currentTimeMillis())
            )
            state.text = "in the deck"
            Toast.makeText(this, "${w.word} added to sona", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }
}
