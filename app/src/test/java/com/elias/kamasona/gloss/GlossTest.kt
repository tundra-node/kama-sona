package com.elias.kamasona.gloss

import com.elias.kamasona.TestDict
import com.elias.kamasona.gloss.Gloss.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GlossTest {

    @Before fun setUp() = TestDict.load()

    private fun Gloss.Result.role(role: Role) = chunks.filter { it.role == role }.map { it.text }

    @Test fun liSplitsSubjectAndPredicate() {
        val r = Gloss.run("jan li moku e kili")
        assertEquals(listOf("jan"), r.role(Role.SUBJECT))
        assertEquals(listOf("moku"), r.role(Role.PREDICATE))
        assertEquals(listOf("kili"), r.role(Role.OBJECT))
    }

    @Test fun bareMiSubjectHasAPredicate() {
        val r = Gloss.run("mi moku")
        assertEquals(listOf("mi"), r.role(Role.SUBJECT))
        assertEquals(listOf("moku"), r.role(Role.PREDICATE))
        assertTrue(r.unknown.isEmpty())
    }

    @Test fun bareSinaWithObject() {
        val r = Gloss.run("sina lukin e tomo")
        assertEquals(listOf("sina"), r.role(Role.SUBJECT))
        assertEquals(listOf("lukin"), r.role(Role.PREDICATE))
        assertEquals(listOf("tomo"), r.role(Role.OBJECT))
    }

    @Test fun oCommand() {
        val r = Gloss.run("o moku e kili")
        assertTrue(r.role(Role.SUBJECT).isEmpty())
        assertEquals(listOf("moku"), r.role(Role.PREDICATE))
        assertEquals(listOf("kili"), r.role(Role.OBJECT))
    }

    @Test fun punctuationAndCaseAreIgnored() {
        val r = Gloss.run("Mi moku e kili.")
        assertTrue(r.unknown.isEmpty())
        assertEquals(listOf("kili"), r.role(Role.OBJECT))
    }

    @Test fun laContext() {
        val r = Gloss.run("tenpo pimeja la mi lape")
        assertEquals(listOf("tenpo pimeja"), r.role(Role.CONTEXT))
        assertEquals(listOf("lape"), r.role(Role.PREDICATE))
    }

    @Test fun unknownWordsAreFlagged() {
        val r = Gloss.run("jan li moku blorp")
        assertEquals(listOf("blorp"), r.unknown)
        assertEquals(listOf("blorp"), r.role(Role.UNKNOWN))
    }

    @Test fun emptyInput() {
        val r = Gloss.run("   ")
        assertTrue(r.chunks.isEmpty())
        assertEquals(1, r.notes.size)
    }
}
