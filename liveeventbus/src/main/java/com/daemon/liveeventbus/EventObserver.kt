package com.daemon.liveeventbus

import androidx.annotation.MainThread
import androidx.lifecycle.Observer

abstract class EventObserver<T> : Observer<T> {
    final override fun onChanged(t: T?) {
        if (t != null) onEvent(t)
    }

    @MainThread
    abstract fun onEvent(event: T)
}