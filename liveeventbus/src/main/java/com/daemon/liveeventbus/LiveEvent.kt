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
            observer: EventObserver<T>
    )

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            observer: EventObserver<T>
    ) = observe(lifecycleOwner, false, observer)

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            sticky: Boolean = false,
            @MainThread observer: (T) -> Unit
    )

    @MainThread
    fun observe(
            lifecycleOwner: LifecycleOwner,
            @MainThread observer: (T) -> Unit
    ) = observe(lifecycleOwner, false, observer)

    @MainThread
    fun removeObserver(observer: EventObserver<T>)

    @MainThread
    fun removeObserver(observer: (T) -> Unit)

    @MainThread
    fun removeObserver(lifecycleOwner: LifecycleOwner)

    @MainThread
    fun clear()
}

internal class LiveEventImpl<T> : LiveEvent<T> {

    private val liveData = MutableLiveData<T>()
    private val observerMap = mutableMapOf<(T) -> Unit, EventObserver<T>>()
    private val liveDataMap = mutableMapOf<EventObserver<T>, LiveData<T>>()

    @SuppressLint("WrongThread")
    override fun emitEvent(event: T) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            liveData.setValue(event)
        else
            liveData.postValue(event)
    }

    override fun observe(lifecycleOwner: LifecycleOwner, sticky: Boolean, observer: (T) -> Unit) {
        val o = object : EventObserver<T>() {
            override fun onEvent(event: T) = observer(event)
        }
        observerMap[observer] = o
        if (!sticky && liveData.value != null) {
            val skip = liveData.skipNoInline()
            liveDataMap[o] = skip
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        observerMap.remove(observer)?.apply {
                            liveDataMap.remove(this)
                        }
                    }
                }
            })
            skip.observe(lifecycleOwner, o)
        } else {
            liveData.observe(lifecycleOwner, o)
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        observerMap.remove(observer)
                    }
                }
            })
        }
    }

    override fun observe(lifecycleOwner: LifecycleOwner, sticky: Boolean, observer: EventObserver<T>) {
        if (!sticky && liveData.value != null) {
            val skip = liveData.skipNoInline()
            liveDataMap[observer] = skip
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        liveDataMap.remove(observer)
                    }
                }
            })
            skip.observe(lifecycleOwner, observer)
        } else {
            liveData.observe(lifecycleOwner, observer)
        }
    }

    override fun removeObserver(observer: EventObserver<T>) {
        liveData.removeObserver(observer)
        @Suppress("UNCHECKED_CAST")
        liveDataMap.remove(observer)?.removeObserver(observer)
    }

    override fun removeObserver(observer: (T) -> Unit) {
        observerMap.remove(observer)?.apply {
            liveData.removeObserver(this)
            liveDataMap.remove(this)?.removeObserver(this)
        }
    }

    override fun removeObserver(lifecycleOwner: LifecycleOwner) {
        liveData.removeObservers(lifecycleOwner)
        liveDataMap.values.forEach {
            it.removeObservers(lifecycleOwner)
        }
        liveDataMap.clear()
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