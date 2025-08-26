package money.vivid.elmslie.core.plot

import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

/** Reducer that doesn't change state, and doesn't emit commands or effects */
class NoOpScheme<State : Any, Event : Any, Effect : Any, Instruction : Any> :
    ElmScheme<State, Event, Effect, Instruction>() {

    override fun SchemePartBuilder<State, Effect, Instruction>.reduce(event: Event) = Unit
}
