package money.vivid.elmslie.core.plot

interface PlotStateObserver<State : Any> {
    fun onStateChanged(state: State?)
}