package money.vivid.elmslie.core.plot

import kotlin.reflect.KClass

interface PlotStateObserver<State : Any> {
    fun onStateChanged(state: State?)
}

interface PlotEffectObserver<Effect : Any> {
    fun onEffectEmitted(effect: Effect)
}

interface Plot<State : Any, Event : Any, Effect : Any> {

    val key: String

    /**
     * Returns the flow of [State]. Internally the store keeps the last emitted state value, so each
     * new subscribers will get it.
     *
     * Note that there will be no emission if a state isn't changed (it's [equals] method returned
     * `true`.
     *
     * By default, [State] is collected in [Dispatchers.Default].
     */
    var currentState: State?
    fun requireState(): State
    fun addStateObserver(observer: PlotStateObserver<State>)
    fun removeStateObserver(observer: PlotStateObserver<State>)

    /**
     * Returns the flow of [Effect]. It's a _hot_ flow and values produced by it **don't cache**.
     *
     * In order to implement cache of [Effect], consider extending [Plot] with appropriate behavior.
     *
     * By default, [Effect] is collected in [Dispatchers.Default].
     */
    fun addEffectObserver(observer: PlotEffectObserver<Effect>)
    fun removeEffectObserver(observer: PlotEffectObserver<Effect>)

    /** Sends a new [Event] for the store. */
    fun accept(event: Event): Pair<State?, List<Effect>>

    /**
     * Stops all operations inside the store and cancels coroutines scope. After this any calls of
     * [accept] method has no effect.
     */
    fun stop()
}

