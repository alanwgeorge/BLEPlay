package com.alangeorge.bleplay.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Pipeline<T> {
    private val _events = MutableSharedFlow<T>()
    val events = _events.asSharedFlow()
    val subscriberCount = _events.subscriptionCount

    suspend fun produceEvent(event: T) {
        _events.emit(event)
    }
}