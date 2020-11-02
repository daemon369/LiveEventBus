package com.daemon.liveeventbus

import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.*

interface LiveEvent<T> {

    @AnyThread
    fun emitEvent(event: T)

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            sticky: Boolean = false,
            observe: EventObserver<T>
    )

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            observe: EventObserver<T>
    ) = observe(lifecycleOwner, false, observe)

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            sticky: Boolean = false,
            @MainThread observe: (T) -> Unit
    ) = observe(lifecycleOwner, sticky, object : EventObserver<T>() {
        override fun onEvent(event: T) {
            observe(event)
        }
    })

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            @MainThread observe: (T) -> Unit
    ) = observe(lifecycleOwner, false, observe)

    @MainThread
    fun removeObserver(observer: EventObserver<T>)

    @MainThread
    fun removeObserver(lifecycleOwner: LifecycleOwner)

    @MainThread
    fun clear()
}

internal class LiveEventImpl<T> : LiveEvent<T> {

    private val liveData = MutableLiveData<T>()
    private val skipLiveData by lazy { liveData.skipNoInline() }

    @SuppressLint("WrongThread")
    override fun emitEvent(event: T) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            liveData.setValue(event)
        else
            liveData.postValue(event)
    }

    override fun observe(lifecycleOwner: LifecycleOwner, sticky: Boolean, observe: EventObserver<T>) {
        if (!sticky && liveData.value != null) {
            skipLiveData.observe(lifecycleOwner, observe)
        } else
            liveData.observe(lifecycleOwner, observe)
    }

    override fun removeObserver(observer: EventObserver<T>) {
        liveData.removeObserver(observer)
        skipLiveData.removeObserver(observer)
    }

    override fun removeObserver(lifecycleOwner: LifecycleOwner) {
        liveData.removeObservers(lifecycleOwner)
        skipLiveData.removeObservers(lifecycleOwner)
    }

    override fun clear() {
        liveData.value = null
    }

}

private fun <T> LiveData<T>.skipNoInline(skipCount: Int = 1): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this, object : Observer<T> {
        var count = 0
        override fun onChanged(t: T?) {
            if (++count > skipCount) {
                result.value = t
            }
        }
    })
    return result
}