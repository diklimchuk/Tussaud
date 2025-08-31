package money.vivid.elmslie.samples.coroutines.timer.elm

import kotlinx.coroutines.Dispatchers
import money.vivid.elmslie.core.plot.ElmPlot

internal fun plotFactory(
    id: String
//) = SingleDispatcherElmPlot(
) = ElmPlot(
    scheme = TimerScheme,
    performer = TimerPerformer,
).also {
    it.accept(Event.Input.Init(id))
}
//    dispatcher = Dispatchers.Main
//)
