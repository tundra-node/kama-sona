package com.elias.kamasona

import com.elias.kamasona.data.Dict
import java.io.File

/** Loads the bundled dictionary once for JVM tests (Gradle runs them from the app/ module dir). */
object TestDict {
    fun load() {
        if (Dict.loaded) return
        Dict.loadJson(File("src/main/assets/dictionary.json").readText())
    }
}
