package money.vivid.elmslie.samples.calculator

import money.vivid.elmslie.core.plot.CoroutinesElmPlot
import money.vivid.elmslie.core.plot.ElmPlot
import money.vivid.elmslie.core.plot.NoOpPerformer
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

private const val MAX_INPUT_LENGTH = 9

val Scheme = object : ElmScheme<State, Event, Effect, Instruction>() {
    override fun SchemePartBuilder<State, Effect, Instruction>.reduce(event: Event) {
        when (event) {
            is Event.EnterDigit -> when {
                state.input.toString().length == MAX_INPUT_LENGTH -> {
                    effects { +Effect.NotifyError("Reached max input length") }
                }

                event.digit.isDigit() -> state { copy(input = "${state.input}${event.digit}".toInt()) }
                else -> effects { +Effect.NotifyError("${event.digit} is not a digit") }
            }

            is Event.PerformOperation -> {
                val total = state.pendingOperation?.invoke(state.total, state.input) ?: state.total
                state { copy(pendingOperation = event.operation, total = total, input = 0) }
                effects { +Effect.NotifyNewResult(total) }
            }

            is Event.Evaluate -> {
                val total = state.pendingOperation?.invoke(state.total, state.input) ?: state.total
                state { copy(pendingOperation = null, total = total, input = 0) }
                effects { +Effect.NotifyNewResult(total) }
            }
        }
    }
}

fun createPlot() = CoroutinesElmPlot(ElmPlot(scheme = Scheme, performer = NoOpPerformer()))
