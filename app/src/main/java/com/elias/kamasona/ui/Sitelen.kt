package com.elias.kamasona.ui

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.elias.kamasona.R
import com.elias.kamasona.data.Word

/**
 * sitelen pona rendering.
 *
 * nasin-nanpa is UCSUR-compliant, but it does not cover every glyph in the
 * dictionary (the nimi sin live in a higher range). Paint.hasGlyph() tells us
 * whether the bundled font can actually draw a codepoint, so we can fall back
 * instead of showing a .notdef box.
 */
object Sitelen {

    private var cached: Typeface? = null
    private var tried = false

    fun typeface(ctx: Context): Typeface? {
        if (!tried) {
            tried = true
            cached = try {
                ResourcesCompat.getFont(ctx, R.font.nasin_nanpa)
            } catch (e: Exception) {
                null
            }
        }
        return cached
    }

    /** Attach the font, then return the text this TextView should show (may be ""). */
    fun glyphText(tv: TextView, word: Word): String {
        val tf = typeface(tv.context) ?: return ""
        tv.typeface = tf
        val glyph = word.effectiveGlyph
        if (glyph.isNotEmpty() && tv.paint.hasGlyph(glyph)) return glyph
        // some words only exist as ligatures in this font
        val lig = word.ligature
        if (lig.isNotEmpty() && tv.paint.hasGlyph(lig)) return lig
        return ""
    }

    fun apply(tv: TextView): Boolean {
        val tf = typeface(tv.context) ?: return false
        tv.typeface = tf
        return true
    }

    /** True when this exact string is drawable by the bundled sitelen pona font. */
    fun canDraw(tv: TextView, text: String): Boolean {
        if (text.isEmpty()) return false
        apply(tv)
        return tv.paint.hasGlyph(text)
    }
}
