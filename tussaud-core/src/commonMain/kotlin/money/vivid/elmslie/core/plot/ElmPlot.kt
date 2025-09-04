package money.vivid.elmslie.core.plot

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import money.vivid.elmslie.core.ElmScope
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.utils.resolvePlotKey
import kotlin.concurrent.Volatile

/**
 * Emits [State]s and [Effect]s on a single dispatcher.
 */
//class SingleDispatcherElmPlot<State : Any, Event : Any, Effect : Any>(
//    private val plot: Plot<State, Event, Effect>,
//    dispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher,
//) : Plot<State, Event, Effect> by plot {
//
//    private val scope = CoroutineScope(
//        context = dispatcher +
//                SupervisorJob() +
//                CoroutineExceptionHandler { context, exception ->
//                    TussaudConfig.logger.fatal("Unhandled error: $exception")
//                } +
//                CoroutineName("${key}SingleDispatcherWrapperPlot")
//    )
//
//    private val stateObservers = ConcurrentMutableMap<PlotStateObserver<State>, PlotStateObserver<State>>()
//    private val effectObservers = ConcurrentMutableMap<PlotEffectObserver<Effect>, PlotEffectObserver<Effect>>()
//
//    override fun addStateObserver(observer: PlotStateObserver<State>) {
//        val observerWrapper = object : PlotStateObserver<State> {
//            override fun onStateChanged(state: State?) {
//                scope.launch {
//                    observer.onStateChanged(state)
//                }
//            }
//        }
//        plot.addStateObserver(observerWrapper)
//        stateObservers[observer] = observerWrapper
//    }
//
//    override fun removeStateObserver(observer: PlotStateObserver<State>) {
//        val observerWrapper = stateObservers[observer] ?: return
//        stateObservers.remove(observer)
//        plot.removeStateObserver(observerWrapper)
//    }
//
//    override fun addEffectObserver(observer: PlotEffectObserver<Effect>) {
//        val observerWrapper = object : PlotEffectObserver<Effect> {
//            override fun onEffectEmitted(effect: Effect) {
//                scope.launch {
//                    observer.onEffectEmitted(effect)
//                }
//            }
//        }
//        plot.addEffectObserver(observerWrapper)
//        effectObservers[observer] = observerWrapper
//    }
//
//    override fun removeEffectObserver(observer: PlotEffectObserver<Effect>) {
//        val observerWrapper = effectObservers[observer] ?: return
//        effectObservers.remove(observer)
//        plot.removeEffectObserver(observerWrapper)
//    }
//
//    override fun accept(event: Event): Pair<State?, List<Effect>> {
//            plot.accept(event)
//        }
//    }
//}

class CoroutinesElmPlot<State : Any, Event : Any, Effect : Any>(
    plot: Plot<State, Event, Effect>,
    dispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher,
) : Plot<State, Event, Effect> by plot, PlotStateObserver<State>, PlotEffectObserver<Effect> {
    private val scope = CoroutineScope(
        context = dispatcher +
                SupervisorJob() +
                CoroutineExceptionHandler { context, exception ->
                    TussaudConfig.logger.fatal("Unhandled error: $exception")
                } +
                CoroutineName("${key}SingleDispatcherWrapperPlot")
    )
    private val internalStates = MutableStateFlow(currentState)
    val states = internalStates.filterNotNull()
    val nullableStates = internalStates.asStateFlow()
    private val internalEffects = MutableSharedFlow<Effect?>(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val effects = internalEffects
        .filterNotNull()

    init {
        addStateObserver(this)
        addEffectObserver(this)
    }

    override fun onStateChanged(state: State?) {
        internalStates.value = state
    }

    override fun onEffectEmitted(effect: Effect) {
        scope.launch {
            internalEffects.emit(effect)
        }
    }

    override fun stop() {
        removeStateObserver(this)
        removeEffectObserver(this)
    }

    inline fun <reified ActualEffect : Effect> acceptWithResult(
        event: Event,
    ): ActualEffect {
        return accept(event)
            .second
            .filterIsInstance<ActualEffect>()
            .also { check(it.size == 1) }
            .first()
    }

    /**
     * Allows to send an event and to observe the result after that
     */
    inline fun <reified ObservedEffect : Effect> acceptAndObserve(event: Event): Flow<ObservedEffect> {
        accept(event)
        return effects.filterIsInstance()
    }
}


@Suppress("TooGenericExceptionCaught")
class ElmPlot<State : Any, Event : Any, Effect : Any, Instruction : Any>(
    private val scheme: ElmScheme<State, Event, Effect, Instruction>,
    private val performer: Performer<Instruction, Event>,
    override val key: String = resolvePlotKey(scheme),
) : Plot<State, Event, Effect> {

    private val logger = TussaudConfig.logger

    /** Store's scope. Active for the lifetime of store. */
    private val scope = ElmScope("${key}Scope")

    // It's probably possible to later make this lock optional to make performance of plot better
    // in an environment where all plots run on the same single threaded dispatcher
    private val lock = SynchronizedObject()
    private val stateObservers = mutableListOf<PlotStateObserver<State>>()

    @Volatile
    override var currentState: State? = null
        set(value) {
            field = value
            stateObservers.forEach { it.onStateChanged(value) }
        }

    private val effectObservers = mutableListOf<PlotEffectObserver<Effect>>()

    override fun requireState(): State = currentState ?: error("State is not defined yet")

    override fun addStateObserver(observer: PlotStateObserver<State>) {
        synchronized(lock) {
            stateObservers.add(observer)
        }
    }

    override fun removeStateObserver(observer: PlotStateObserver<State>) {
        synchronized(lock) {
            stateObservers.remove(observer)
        }
    }

    override fun addEffectObserver(observer: PlotEffectObserver<Effect>) {
        synchronized(lock) {
            effectObservers.add(observer)
        }
    }

    override fun removeEffectObserver(observer: PlotEffectObserver<Effect>) {
        synchronized(lock) {
            effectObservers.add(observer)
        }
    }

    inline fun <reified ActualEffect : Effect> acceptWithResult(
        event: Event,
    ): ActualEffect {
        return accept(event)
            .second
            .filterIsInstance<ActualEffect>()
            .also { check(it.size == 1) }
            .first()
    }

    override fun accept(event: Event): Pair<State?, List<Effect>> {
        synchronized(lock) {
            try {
                val oldState = currentState
                logger.debug(message = "New event: $event", tag = key)
                val (state, effects, commands) = scheme.reduce(oldState, event)
                currentState = state
                effects.forEach { effect -> dispatchEffect(effect) }
                commands.forEach { executeCommand(it) }
                return state to effects
            } catch (t: Throwable) {
                logger.fatal(message = "You must handle all errors inside reducer", tag = key, error = t)
                return currentState to emptyList()
            }
        }
    }

    override fun stop() {
        scope.cancel()
    }

    private fun dispatchEffect(effect: Effect) {
        logger.debug(message = "New effect: $effect", tag = key)
        effectObservers.forEach { it.onEffectEmitted(effect) }
    }

    private fun executeCommand(command: Instruction) {
        scope.launch {
            logger.debug(message = "Executing command: $command", tag = key)
            performer
                .execute(command)
                .onEach { logger.debug(message = "Command $command produces event $it", tag = key) }
                .cancellable()
                .catch { throwable ->
                    logger.nonfatal(
                        message = "Unhandled exception inside the command $command",
                        tag = key,
                        error = throwable,
                    )
                }
                .collect { accept(it) }
        }
    }
}

fun <Event : Any, State : Any, Effect : Any> Plot<Event, State, Effect>.toCachedPlot() =
    EffectCachingElmPlot(this)
