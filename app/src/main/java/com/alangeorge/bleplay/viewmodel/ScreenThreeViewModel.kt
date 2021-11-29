package com.alangeorge.bleplay.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.*
import arrow.core.left
import arrow.core.right
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.repository.BleOperationRequest
import com.alangeorge.bleplay.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScreenThreeViewModel @Inject constructor(
    private val repository: BleRepository,
    private val snackbarMessagePipeline: Pipeline<SnackbarMessage>
) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.bleOperation(BleOperationRequest(1)).fold(
                {
                    Timber.d("[${Thread.currentThread().name}] repository result $it")
                    _title.postValue(it.result)
                },
                {
                    Timber.e(it, "repository error")
                }
            )
            repository.events.collect {
                _data.postValue(it)
            }
        }
    }

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title
    private val _data = MutableLiveData<Int>()
    val data: LiveData<Int> = _data

    fun displaySnackbar(@StringRes messageId: Int) {
        viewModelScope.launch {
            snackbarMessagePipeline.produceEvent(SnackbarMessage(messageId.left()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared()")
    }
}