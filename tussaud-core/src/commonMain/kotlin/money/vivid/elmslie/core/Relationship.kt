package money.vivid.elmslie.core

import money.vivid.elmslie.core.plot.Plot
import money.vivid.elmslie.core.plot.PlotEffectObserver
import money.vivid.elmslie.core.plot.PlotStateObserver

fun <PlotState : Any, PlotEvent : Any, PlotEffect : Any,
        OtherPlotState : Any, OtherPlotEvent : Any, OtherPlotEffect : Any>
        Plot<PlotState, PlotEvent, PlotEffect>.relateTo(
    otherPlot: Plot<OtherPlotState, OtherPlotEvent, OtherPlotEffect>,
    stateToOtherEventMapper: (PlotState?) -> OtherPlotEvent,
    otherStateToEventMapper: (OtherPlotState?) -> PlotEvent,
    effectToOtherEventMapper: (PlotEffect?) -> OtherPlotEvent,
    otherEffectToEventMapper: (OtherPlotEffect?) -> PlotEvent,
) {
    addStateObserver(object : PlotStateObserver<PlotState> {
        override fun onStateChanged(state: PlotState?) {
            otherPlot.accept(stateToOtherEventMapper(state))
        }
    })
    otherPlot.addStateObserver(object : PlotStateObserver<OtherPlotState> {
        override fun onStateChanged(state: OtherPlotState?) {
            accept(otherStateToEventMapper(state))
        }
    })
    addEffectObserver(object : PlotEffectObserver<PlotEffect> {
        override fun onEffectEmitted(effect: PlotEffect) {
            otherPlot.accept(effectToOtherEventMapper(effect))
        }
    })
    otherPlot.addEffectObserver(object : PlotEffectObserver<OtherPlotEffect> {
        override fun onEffectEmitted(effect: OtherPlotEffect) {
            accept(otherEffectToEventMapper(effect))
        }
    })
}