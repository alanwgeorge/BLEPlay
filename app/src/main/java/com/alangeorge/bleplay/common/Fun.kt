package com.alangeorge.bleplay.common

infix operator fun (() -> Unit).plus(that: () -> Unit): () -> Unit = {
    this()
    that()
}
infix  fun (() -> Unit).then(that: () -> Unit): () -> Unit = {
    this()
    that()
}