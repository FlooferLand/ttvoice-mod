package com.flooferland.espeak

import io.kotest.core.spec.style.FunSpec

class EspeakLibTest : FunSpec({
    test("initialize") {
        Espeak.initialize()
    }

    test("speak") {
        Espeak.initialize()
        Espeak.synth("Hello World!")
    }
})