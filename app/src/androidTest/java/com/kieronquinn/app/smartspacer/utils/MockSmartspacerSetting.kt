package com.kieronquinn.app.smartspacer.utils

import com.kieronquinn.app.smartspacer.repositories.BaseSettingsRepository.FakeSmartspacerSetting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

inline fun <reified T> mockSmartspacerSetting(initialValue: T): MockSmartspacerSetting<T> {
    val flow = MutableStateFlow(initialValue)
    return MockSmartspacerSetting(T::class.java, flow)
}

class MockSmartspacerSetting<T>(
    type: Class<T>,
    private val flow: MutableStateFlow<T>
): FakeSmartspacerSetting<T>(type, flow, { flow.emit(it) }), MutableStateFlow<T> {

    override val replayCache: List<T>
        get() = flow.replayCache

    override var value: T
        set(value) {
            flow.value = value
        }
        get() = flow.value

    override val subscriptionCount: StateFlow<Int>
        get() = flow.subscriptionCount

    override fun compareAndSet(expect: T, update: T): Boolean {
        return flow.compareAndSet(expect, update)
    }

    override suspend fun emit(value: T) {
        flow.emit(value)
    }

    @ExperimentalCoroutinesApi
    override fun resetReplayCache() {
        flow.resetReplayCache()
    }

    override fun tryEmit(value: T): Boolean {
        return flow.tryEmit(value)
    }

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        flow.collect(collector)
    }

}