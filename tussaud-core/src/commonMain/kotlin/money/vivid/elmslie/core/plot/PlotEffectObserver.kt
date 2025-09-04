package money.vivid.elmslie.core.plot

interface PlotEffectObserver<Effect : Any> {
    fun onEffectEmitted(effect: Effect)
}