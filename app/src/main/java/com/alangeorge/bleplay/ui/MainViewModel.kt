package com.alangeorge.bleplay.ui

import androidx.lifecycle.*
import com.alangeorge.bleplay.repository.BleOperationRequest
import com.alangeorge.bleplay.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BleRepository
) : ViewModel(), LifecycleEventObserver {
    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title
    private val _data = MutableLiveData<Int>()
    val data: LiveData<Int> = _data

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_CREATE -> {
                source.lifecycle.addObserver(repository)
                source.lifecycleScope.launch(Dispatchers.IO) {
                    repository.bleOperation(BleOperationRequest(1)).fold(
                        {
                            Timber.d("[${Thread.currentThread().name}] repository result $it")
                            _title.postValue(it.result)
                        },
                        {
                            Timber.e(it, "repository error")
                        }
                    )
                }

                source.lifecycleScope.launch {
                    repository.events.collect {
                        _data.postValue(it)
                    }
                }
            }
            else -> {}
        }
    }
}