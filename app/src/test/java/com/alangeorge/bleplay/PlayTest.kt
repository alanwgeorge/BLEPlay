package com.alangeorge.bleplay

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class PlayTest {
    @ExperimentalTime
    @Test
    fun `flow combine play`() = runBlocking {
        val numbersFlow = flowOf(1,2,3).onEach { delay(1000) }
        val lettersFlow = flowOf("A", "B","C").onEach { delay(2000) }

        numbersFlow.combineTransform(lettersFlow) { n, l ->
            emit("$n$l")
        }.collect { s ->
            println(s)
        }
    }
}