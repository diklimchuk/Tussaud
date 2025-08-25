package money.vivid.elmslie.samples.coroutines.timer.elm

import java.util.UUID
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder
import money.vivid.elmslie.samples.coroutines.timer.elm.Event.Input
import money.vivid.elmslie.samples.coroutines.timer.elm.Event.Result

internal object TimerScheme : ElmScheme<State, Event, Effect, Instruction>() {
    override fun SchemePartBuilder<State, Effect, Instruction>.reduce(
        event: Event
    ) {
        when (event) {
            is Input.Init -> {
                nullableState {
                    State(
                        id = event.id,
                        isStarted = true,
                        generatedId = UUID.randomUUID().toString()
                    )
                }
                instructions { +Instruction.Start }
            }

            is Input.Start -> {
                state { copy(isStarted = true) }
                instructions { +Instruction.Start }
            }

            is Input.Stop -> {
                state { copy(isStarted = false) }
                instructions { +Instruction.Stop }
            }

            is Result.OnTimeTick -> {
                state { copy(secondsPassed = secondsPassed + 1) }
            }

            is Result.OnTimeError -> {
                state { copy(isStarted = false) }
                effects { +Effect.Error(event.throwable) }
            }
        }
    }
}
