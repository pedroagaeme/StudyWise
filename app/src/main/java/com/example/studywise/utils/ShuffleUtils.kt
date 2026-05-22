package com.example.studywise.utils

import kotlin.random.Random

fun <T> List<T>.shuffledWithHash(seed: String): List<T> {
    val mixedSeed = seed.toMixedSeed()
    return shuffled(Random(mixedSeed))
}

private fun String.toMixedSeed(): Long {
    var value = hashCode().toLong()
    value = value * -7046029254386353131L + length.toLong()
    value = (value xor (value ushr 30)) * -4658895280553007687L
    value = (value xor (value ushr 27)) * -7723592293110705685L
    return value xor (value ushr 31)
}



