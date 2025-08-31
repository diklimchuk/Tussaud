package money.vivid.elmslie.core.plot

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import money.vivid.elmslie.core.ElmScope
import money.vivid.elmslie.core.config.TussaudConfig

/**
 * Caches effects until there is at least one collector.
 *
 * Note, that effects from the cache are replayed only for the first one.
 *
 * Wrap the store with the instance of [EffectCachingElmPlot] to get the desired behavior like
 * this:
 * ```
 * ```
 */
// TODO Should be moved to android artifact?
class EffectCachingElmPlot<State : Any, Event : Any, Effect : Any>(
    private val internalPlot: Plot<State, Event, Effect>
) : Plot<State, Event, Effect> by internalPlot {

    private val plot = CoroutinesElmPlot(
        internalPlot,
        dispatcher = TussaudConfig.elmDispatcher,
    )
    private val effectsMutex = Mutex()
    private val effectsCache = mutableListOf<Effect>()
    private val effectsFlow = MutableSharedFlow<Effect>()
    private val plotScope = ElmScope("CachedPlotScope")

    init {
        plotScope.launch {
            plot.effects.collect { effect ->
                if (effectsFlow.subscriptionCount.value > 0) {
                    effectsFlow.emit(effect)
                } else {
                    effectsMutex.withLock { effectsCache.add(effect) }
                }
            }
        }
    }

    override fun stop() {
        internalPlot.stop()
        plotScope.cancel()
    }

    val effects: Flow<Effect> = effectsFlow.onSubscription {
        effectsMutex.withLock {
            for (effect in effectsCache) {
                emit(effect)
            }
            effectsCache.clear()
        }
    }
}
