package com.alangeorge.bleplay.common

infix operator fun (() -> Unit).plus(that: () -> Unit): () -> Unit = {
    this()
    that()
}
infix  fun (() -> Unit).then(that: () -> Unit): () -> Unit = {
    this()
    that()
}

inline infix fun <P1, R> P1.pipe(t: (P1) -> R) :R = t(this)