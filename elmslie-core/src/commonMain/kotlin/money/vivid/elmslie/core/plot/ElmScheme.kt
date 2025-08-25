package money.vivid.elmslie.core.plot

import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

abstract class ElmScheme<State : Any, Event : Any, Effect : Any, Instruction : Any> {

    protected abstract fun SchemePartBuilder<State, Effect, Instruction>.reduce(event: Event)

    fun reduce(state: State?, event: Event) = SchemePartBuilder<State, Effect, Instruction>(state).apply { reduce(event) }.build()
}
