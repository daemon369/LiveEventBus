package com.daemon.liveeventbus

import androidx.annotation.AnyThread
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@AnyThread
object LiveEventBus {

    private val map = ConcurrentHashMap<Class<*>, LiveEventImpl<*>>()

    fun <T> with(eventType: Class<T>): LiveEvent<T> {
        var liveEvent = map[eventType]
        if (liveEvent == null) {
            liveEvent = LiveEventImpl<T>()
            @Suppress("UNCHECKED_CAST")
            val l = map.putIfAbsent(eventType, liveEvent) as LiveEventImpl<T>?
            if (l != null)
                liveEvent = l
        }
        @Suppress("UNCHECKED_CAST")
        return liveEvent as LiveEvent<T>
    }

    fun <T : Any> with(eventType: KClass<T>): LiveEvent<T> =
            with(eventType.java)

    inline fun <reified T : Any> emit(event: T) =
            with(event.javaClass).emitEvent(event)

}