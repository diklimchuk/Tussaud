package money.vivid.elmslie.core.plot.methodical

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import money.vivid.elmslie.core.ElmScope
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.plot.Plot
import money.vivid.elmslie.core.plot.PlotEffectObserver
import money.vivid.elmslie.core.plot.PlotStateObserver
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder
import kotlin.concurrent.Volatile

// TODO: Rename instruction to operation across the entire library
class MethodicalElmPlot<
        State : Any,
        Effect : Any,
        Operation : (Resources) -> Flow<Event>,
        Event : (SchemePartBuilder<State, Effect, Operation>) -> SchemePartBuilder<State, Effect, Operation>,
        Resources : Any,
        >(
    override val key: String = "", // TODO: resolvePlotKey(scheme),
    private val resources: Resources,
) : Plot<State, Event, Effect> {

    private val scheme = MethodicalElmScheme<State, Effect, Operation, Resources, Event>()
    private val performer = MethodicalPerformer<Operation, Resources, Event>()

    private val logger = TussaudConfig.logger

    /** Store's scope. Active for the lifetime of store. */
    private val scope = ElmScope("${key}Scope")

    private val lock = SynchronizedObject()

    private val stateObservers = mutableListOf<PlotStateObserver<State>>()
    private val effectObservers = mutableListOf<PlotEffectObserver<Effect>>()

    @Volatile
    override var currentState: State? = null
        set(value) {
            field = value
            stateObservers.forEach { it.onStateChanged(value) }
        }

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
                logger.fatal(
                    message = "You must handle all errors inside reducer",
                    tag = key,
                    error = t
                )
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

    private fun executeCommand(command: Operation) {
        // TODO:
        scope.launch {
            logger.debug(message = "Executing command: $command", tag = key)
            performer
                .execute(resources, command)
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