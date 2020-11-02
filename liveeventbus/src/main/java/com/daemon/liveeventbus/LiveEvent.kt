package com.daemon.liveeventbus

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
            sticky: Boolean = false,
            @MainThread observe: (T) -> Unit
    ) {
        observe(lifecycleOwner, sticky, object : EventObserver<T>() {
            override fun onEvent(event: T) {
                observe(event)
            }
        })
    }

    @MainThread
    fun observe(lifecycleOwner: LifecycleOwner, @MainThread observe: (T) -> Unit) {
        observe(lifecycleOwner, false, object : EventObserver<T>() {
            override fun onEvent(event: T) {
                observe(event)
            }
        })
    }

    @MainThread
    fun removeObserver(observer: EventObserver<T>)

    @MainThread
    fun removeObserver(lifecycleOwner: LifecycleOwner)

    @MainThread
    fun clear()
}

internal class LiveEventImpl<T> : LiveEvent<T> {

    private val liveData = MutableLiveData<T>()

    override fun emitEvent(event: T) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            liveData.setValue(event)
        else
            liveData.postValue(event)
    }

    override fun observe(lifecycleOwner: LifecycleOwner, sticky: Boolean, observe: EventObserver<T>) {
        if (!sticky && liveData.value != null)
            (liveData.skipNoInline(1) as MutableLiveData<T>).observe(lifecycleOwner, observe)
        else
            liveData.observe(lifecycleOwner, observe)
    }

    override fun removeObserver(observer: EventObserver<T>) {
        liveData.removeObserver(observer)
    }

    override fun removeObserver(lifecycleOwner: LifecycleOwner) {
        liveData.removeObservers(lifecycleOwner)
    }

    override fun clear() {
        liveData.value = null
    }

}

fun <T> LiveData<T>.skipNoInline(skipCount: Int = 1): LiveData<T> {
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