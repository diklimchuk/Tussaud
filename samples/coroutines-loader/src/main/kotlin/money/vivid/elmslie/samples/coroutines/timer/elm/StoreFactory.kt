package money.vivid.elmslie.samples.coroutines.timer.elm

import kotlinx.coroutines.Dispatchers
import money.vivid.elmslie.core.plot.ElmPlot
import money.vivid.elmslie.core.plot.SingleDispatcherSchemeElmPlot

internal fun plotFactory(
    id: String
) = SingleDispatcherSchemeElmPlot(
    plot = ElmPlot(
        scheme = TimerScheme,
        performer = TimerPerformer,
    ).also {
        it.accept(Event.Input.Init(id))
    },
    dispatcher = Dispatchers.Main
)
