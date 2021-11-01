package com.alangeorge.bleplay.repository

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class BleRepository : LifecycleEventObserver {
    init {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                _events.emit(data++)
            }
        }
    }

    private val _events = MutableSharedFlow<Int>(0)
    private var data: Int = 1

    val events: SharedFlow<Int> = _events

    suspend fun bleOperation(request: BleOperationRequest): Result<BleOperationResponse> = runCatching {
        delay(1000)
        BleOperationResponse("foobar${request.action}")
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_CREATE -> {
                source.lifecycleScope.launch {
                    while (true) {
                        delay(1000)
                        _events.emit(data++)
                    }
                }.invokeOnCompletion {
                    Timber.d("flow completed")
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                Timber.d("lifecycle destroyed")
            }
            else -> {}
        }
    }
}

data class BleOperationRequest(val action: Int)
data class BleOperationResponse(val result: String)