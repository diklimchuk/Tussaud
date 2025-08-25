package money.vivid.elmslie.samples.coroutines.timer.elm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.plot.Performer

internal object TimerPerformer : Performer<Instruction, Event>() {

    override fun execute(instruction: Instruction) =
        when (instruction) {
            is Instruction.Start ->
                secondsFlow()
                    .switch()
                    .mapEvents(
                        onResult = { Event.Result.OnTimeTick },
                        onError = Event.Result::OnTimeError
                    )

            is Instruction.Stop -> cancelSwitchFlows<Instruction.Start>().mapEvents()
        }

    @Suppress("MagicNumber")
    private fun secondsFlow(): Flow<Int> = flow {
        repeat(10) {
            delay(1000)
            emit(it)
        }
        error("Test error")
    }
}
