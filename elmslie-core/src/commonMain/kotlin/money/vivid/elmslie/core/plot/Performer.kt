package money.vivid.elmslie.core.plot

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import money.vivid.elmslie.core.config.ElmslieConfig
import money.vivid.elmslie.core.switcher.Switcher

abstract class Performer<Instruction : Any, Result : Any> {

    private val switchers = mutableMapOf<Any, Switcher>()
    private val mutex = Mutex()

    /** Executes a command. This method is performed on the [ElmslieConfig.elmDispatcher]. */
    abstract fun execute(instruction: Instruction): Flow<Result>

    protected fun <T : Any> Flow<T>.mapEvents(
        onResult: (T) -> Result? = { null },
        onError: (error: Throwable) -> Result? = { null },
    ) = mapNotNull(onResult)
        .catch {
            it.logErrorEvent(onError)
                ?.let { event -> emit(event) }
                ?: throw it
        }

    protected inline fun <reified T : Any> Flow<T>.switch(
        delay: Duration = 0.milliseconds
    ): Flow<T> {
        return switchByKey(T::class, delay)
    }

    /**
     * Extension function to switch the flow by a given key and optional delay. This function ensures
     * that only one flow with the same key is active at a time.
     *
     * @param key The key to identify the flow.
     * @param delay The delay in milliseconds before launching the initial flow. Defaults to 0.
     * @return A new flow that emits the values from the original flow.
     */
    protected fun <T : Any> Flow<T>.switchByKey(
        key: Any,
        delay: Duration = 0.milliseconds
    ): Flow<T> {
        return flow {
            val switcher = mutex.withLock { switchers.getOrPut(key) { Switcher() } }
            switcher.switch(delay) { this@switchByKey }.collect { emit(it) }
        }
    }

    protected inline fun <reified T> cancelSwitchFlows(
        vararg keys: Any
    ): Flow<Unit> {
        return cancelSwitchFlowsByArg(T::class, *keys)
    }

    /**
     * Cancels the switch flow(s) by a given key(s).
     *
     * @param keys The keys to identify the flows.
     * @return A new flow that emits [Unit] when switch flows are cancelled.
     */
    protected fun cancelSwitchFlowsByArg(
        vararg keys: Any
    ): Flow<Unit> {
        return flow {
            keys.forEach { key -> mutex.withLock { switchers.remove(key)?.cancel() } }
            emit(Unit)
        }
    }

    private fun Throwable.logErrorEvent(
        onError: (Throwable) -> Result?
    ): Result? {
        return onError(this).also { ElmslieConfig.logger.nonfatal(error = this) }
    }
}
