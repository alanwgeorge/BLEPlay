package com.alangeorge.bleplay.repository

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class BleRepository {
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
}

data class BleOperationRequest(val action: Int)
data class BleOperationResponse(val result: String)