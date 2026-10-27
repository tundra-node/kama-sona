package com.elias.kamasona.data

import com.elias.kamasona.TestDict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictTest {

    @Before fun setUp() = TestDict.load()

    @Test fun loadsWordsAndGlyphs() {
        assertTrue(Dict.words.size > 100)
        assertTrue(Dict.glyphs.isNotEmpty())
        assertTrue(Dict.attribution.contains("CC BY-SA"))
        assertNotNull(Dict.word("toki"))
    }

    @Test fun exactMatchRanksFirst() {
        assertEquals("moku", Dict.search("moku").first().word)
    }

    @Test fun searchesEnglishDefinitions() {
        assertTrue(Dict.search("fruit").any { it.word == "kili" })
    }

    @Test fun categoryFilter() {
        assertTrue(Dict.search("", "obscure").all { it.category == "obscure" })
    }

    @Test fun shortDefIsFirstSense() {
        assertEquals("eat, drink, consume, swallow, ingest", Dict.word("moku")!!.shortDef)
    }

    @Test fun variantsPutPrimaryFirst() {
        val vs = Dict.variants("wile")
        assertTrue(vs.isNotEmpty())
        assertTrue(vs.first().primary || vs.none { it.primary })
    }

    @Test fun ucsurConversion() {
        assertEquals(String(Character.toChars(0xF1936)), ucsurToChar("U+F1936"))
        assertEquals("", ucsurToChar(""))
        assertEquals("", ucsurToChar("U+zzzz"))
    }
}
