package money.vivid.elmslie.core.plot.dsl

import money.vivid.elmslie.core.plot.SchemePart

open class SchemePartBuilder<State : Any, Effect : Any, Instruction : Any>(
    val nullableInitialState: State?,
) {

    val initialState: State
        get() = nullableInitialState ?: error("Initial state was null")
    private var currentState: State? = nullableInitialState
    private val instructionsBuilder = OperationsBuilder<Instruction>()
    private val effectsBuilder = OperationsBuilder<Effect>()

    val nullableState: State?
        get() = currentState

    val state
        get() = currentState ?: error("State is not defined yet")

    fun nullableState(update: State?.() -> State?) {
        currentState = currentState.update()
    }

    fun state(update: State.() -> State?) {
        currentState = state.update()
    }

    fun instructions(update: OperationsBuilder<Instruction>.() -> Unit) {
        instructionsBuilder.update()
    }

    fun effects(update: OperationsBuilder<Effect>.() -> Unit) {
        effectsBuilder.update()
    }

    internal fun build(): SchemePart<State, Effect, Instruction> {
        return SchemePart(currentState, effectsBuilder.build(), instructionsBuilder.build())
    }
}
