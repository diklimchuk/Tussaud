package money.vivid.elmslie.android.renderer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import money.vivid.elmslie.core.config.TussaudConfig
import money.vivid.elmslie.core.plot.CoroutinesElmPlot
import money.vivid.elmslie.core.plot.Plot

class ElmRenderer<State : Any, Effect : Any>(
    internalPlot: Plot<State, *, Effect>,
    private val delegate: ElmRendererDelegate<State, Effect>,
    private val lifecycle: Lifecycle,
) {

    private val elmDispatcher: CoroutineDispatcher = TussaudConfig.elmDispatcher
    private val plot = CoroutinesElmPlot(
        plot = internalPlot,
        dispatcher = elmDispatcher
    )
    private val logger = TussaudConfig.logger
    private val canRender
        get() = lifecycle.currentState.isAtLeast(STARTED)

    init {
        with(lifecycle) {
            coroutineScope.launch {
                plot.effects
                    .flowWithLifecycle(
                        lifecycle = lifecycle,
                        minActiveState = RESUMED
                    )
                    .collect { effect ->
                        catchEffectErrors { delegate.handleEffect(effect) }
                    }
            }
            coroutineScope.launch {
                plot.states
                    .flowOn(elmDispatcher)
                    .flowWithLifecycle(
                        lifecycle = lifecycle,
                        minActiveState = STARTED
                    )
                    .map { state -> state to mapListItems(state) }
                    .catch { logger.fatal(message = "Crash while mapping state", error = it) }
                    .flowOn(Dispatchers.Main)
                    .collect { (state, listItems) ->
                        catchStateErrors {
                            if (canRender) {
                                delegate.renderList(state, listItems)
                                delegate.render(state)
                            }
                        }
                    }
            }
        }
    }

    private fun mapListItems(
        state: State
    ) = catchStateErrors { delegate.mapList(state) } ?: emptyList()

    @Suppress("TooGenericExceptionCaught")
    private fun <T> catchStateErrors(
        action: () -> T?
    ) = try {
        action()
    } catch (t: Throwable) {
        logger.fatal(message = "Crash while rendering state", error = t)
        null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> catchEffectErrors(
        action: () -> T?
    ) = try {
        action()
    } catch (t: Throwable) {
        logger.fatal(message = "Crash while handling effect", error = t)
    }
}
