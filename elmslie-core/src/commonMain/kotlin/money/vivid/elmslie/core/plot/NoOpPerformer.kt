package money.vivid.elmslie.core.plot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Actor that doesn't emit any events after receiving a command */
class NoOpPerformer<Event : Any, Instruction : Any> : Performer<Instruction, Event>() {

  override fun execute(instruction: Instruction): Flow<Event> = emptyFlow()
}
