package com.example.studywise.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ShuffleUtilsTest {
    @Test
    fun shuffledWithHash_isDeterministicForSameSeed() {
        val input = listOf(1, 2, 3, 4, 5)
        val seed = "attempt-123:question-456"

        val first = input.shuffledWithHash(seed)
        val second = input.shuffledWithHash(seed)

        assertEquals(first, second)
        assertEquals(input.sorted(), first.sorted())
    }

    @Test
    fun shuffledWithHash_handlesEmptyList() {
        val result = emptyList<Int>().shuffledWithHash("any-seed")

        assertEquals(emptyList<Int>(), result)
    }
}

