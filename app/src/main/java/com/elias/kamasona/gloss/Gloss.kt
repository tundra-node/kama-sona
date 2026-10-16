package com.elias.kamasona.gloss

import com.elias.kamasona.data.Dict

/**
 * A deliberately small toki pona glosser.
 *
 * It does not translate. It splits a sentence into its grammatical parts and
 * labels each word, so a learner can see *why* a sentence works the way it does.
 * Anything it cannot classify is marked and left alone rather than guessed at.
 */
object Gloss {

    enum class Role { SUBJECT, PREDICATE, OBJECT, PREP_PHRASE, CONTEXT, MODIFIER, PARTICLE, PREVERB, PREPOSITION, UNKNOWN }

    data class Chunk(val text: String, val role: Role, val note: String)

    data class Result(
        val chunks: List<Chunk>,
        val notes: List<String>,
        val unknown: List<String>,
    )

    private val SUBJECT_PRONOUNS = setOf("mi", "sina")
    private val PREVERBS = setOf("wile", "ken", "kama", "awen", "sona", "lukin", "open", "pini")
    private val PREPOSITIONS = setOf("kepeken", "tawa", "tan", "lon", "sama", "sike", "nasin")
    private val PARTICLES = setOf("li", "e", "la", "pi", "en", "anu", "o", "n", "kin")
    private val PRONOUNS = setOf("mi", "sina", "ona", "ni", "mi mute", "sina mute")
    private val NUMBERS = setOf("wan", "tu", "mute", "ale", "ali", "ala", "luka", "mute")

    fun run(sentence: String): Result {
        val raw = sentence.trim()
        if (raw.isEmpty()) return Result(emptyList(), listOf("write a sentence first"), emptyList())

        val notes = ArrayList<String>()
        val unknown = ArrayList<String>()
        val chunks = ArrayList<Chunk>()

        // 1. split off a la-phrase (context) if present
        val laIdx = raw.indexOf(" la ")
        val body: String
        if (laIdx > 0) {
            val ctx = raw.substring(0, laIdx).trim()
            chunks.add(Chunk(ctx, Role.CONTEXT, "context clause (\"$ctx\" la ...)"))
            body = raw.substring(laIdx + 4).trim()
            notes.add("la marks what comes before it as context for the sentence after it.")
        } else {
            body = raw
        }

        // 2. split into predicate groups: one before e, one after (objects)
        val tokens = body.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return Result(chunks, notes, unknown)

        val eIdx = tokens.indexOfFirst { it == "e" }
        val subjectPart: List<String>
        val predicatePart: List<String>
        val objectPart: List<String>

        val liIdx = tokens.indexOfFirst { it == "li" }
        if (liIdx >= 0) {
            subjectPart = tokens.subList(0, liIdx)
            val rest = tokens.subList(liIdx + 1, tokens.size)
            val e = rest.indexOfFirst { it == "e" }
            if (e >= 0) {
                predicatePart = rest.subList(0, e)
                objectPart = rest.subList(e + 1, rest.size)
            } else {
                predicatePart = rest
                objectPart = emptyList()
            }
            notes.add("li separates the subject from what is said about it.")
        } else if (eIdx >= 0) {
            subjectPart = tokens.subList(0, eIdx)
            predicatePart = emptyList()
            objectPart = tokens.subList(eIdx + 1, tokens.size)
            notes.add("no li here — the subject is mi or sina, or the sentence is an o-command.")
        } else {
            subjectPart = tokens
            predicatePart = emptyList()
            objectPart = emptyList()
        }

        fun describe(parts: List<String>, role: Role) {
            if (parts.isEmpty()) return
            val head = parts.first()
            val rest = parts.drop(1)
            val headGloss = Dict.word(head)?.shortDef ?: "?"
            when (role) {
                Role.SUBJECT -> {
                    val who = if (head in SUBJECT_PRONOUNS) "speaker/addressee (no li needed)" else "the head noun"
                    chunks.add(Chunk(parts.joinToString(" "), role, "subject: $headGloss — $who"))
                }
                Role.PREDICATE -> {
                    val pre = if (head in PREVERBS) "preverb (starts the verb phrase): " else ""
                    chunks.add(Chunk(parts.joinToString(" "), role, "predicate: $pre$headGloss"))
                }
                Role.OBJECT -> {
                    chunks.add(Chunk(parts.joinToString(" "), role, "object: $headGloss (introduced by e)"))
                }
                else -> {}
            }
            for (word in rest) {
                if (word in PARTICLES) continue
                if (word in PREPOSITIONS) {
                    chunks.add(Chunk(word, Role.PREPOSITION, "preposition: ${Dict.word(word)?.shortDef ?: "?"}"))
                } else if (word in PREVERBS) {
                    chunks.add(Chunk(word, Role.PREVERB, "preverb: ${Dict.word(word)?.shortDef ?: "?"}"))
                } else if (word == "ala") {
                    chunks.add(Chunk(word, Role.PARTICLE, "negation / question marker"))
                } else {
                    val g = Dict.word(word)
                    if (g == null) {
                        unknown.add(word)
                        chunks.add(Chunk(word, Role.UNKNOWN, "not a toki pona word I know"))
                    } else {
                        chunks.add(Chunk(word, Role.MODIFIER, "modifier of \"$head\": ${g.shortDef}"))
                    }
                }
            }
        }

        describe(subjectPart, Role.SUBJECT)
        describe(predicatePart, Role.PREDICATE)
        describe(objectPart, Role.OBJECT)

        // 3. flag prepositional phrases that appear mid-sentence
        for ((i, t) in tokens.withIndex()) {
            if (t in PREPOSITIONS && i > 0) {
                val after = tokens.drop(i + 1).takeWhile { it !in PARTICLES && it !in PREPOSITIONS }
                if (after.isNotEmpty() && chunks.none { it.text == "${t} ${after.joinToString(" ")}" }) {
                    chunks.add(
                        Chunk("$t ${after.joinToString(" ")}", Role.PREP_PHRASE,
                            "prepositional phrase: ${Dict.word(t)?.shortDef ?: t} …")
                    )
                }
            }
        }

        if (chunks.none { it.role == Role.PREDICATE }) {
            notes.add("no li and no e — this reads as a naming sentence (\"X is Y\") or a fragment.")
        }
        val hasAlaAfterVerb = tokens.windowed(2).any { it[0] !in PARTICLES && it[1] == "ala" }
        if (hasAlaAfterVerb) {
            notes.add("\"ala\" after a word negates it; after a predicate it can also form a yes/no question.")
        }
        if (tokens.contains("pi")) {
            notes.add("pi regroups modifiers: the words after pi modify together as one unit.")
        }
        return Result(chunks, notes, unknown)
    }
}
