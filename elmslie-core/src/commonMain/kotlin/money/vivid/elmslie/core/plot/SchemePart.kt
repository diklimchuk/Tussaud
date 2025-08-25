package money.vivid.elmslie.core.plot

/** Represents result of reduce function */
data class SchemePart<State : Any, Effect : Any, Instruction : Any>(
  val state: State?,
  val effects: List<Effect>,
  val instructions: List<Instruction>,
) {

  constructor(
    state: State?,
    effect: Effect? = null,
    command: Instruction? = null,
  ) : this(state = state, effects = listOfNotNull(effect), instructions = listOfNotNull(command))

  constructor(
    state: State?,
    commands: List<Instruction>,
  ) : this(state = state, effects = emptyList(), instructions = commands)

  constructor(state: State?) : this(state = state, effects = emptyList(), instructions = emptyList())
}
