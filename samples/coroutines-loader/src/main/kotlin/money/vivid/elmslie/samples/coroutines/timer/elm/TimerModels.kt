package money.vivid.elmslie.samples.coroutines.timer.elm

internal data class State(
  val id: String,
  val secondsPassed: Long = 0,
  val generatedId: String? = null,
  val isStarted: Boolean = false,
)

internal sealed class Effect {
  data class Error(val throwable: Throwable) : Effect()
}

internal sealed class Instruction {
  object Start : Instruction()

  object Stop : Instruction()
}

internal sealed class Event {
  internal sealed class Input : Event(){
    data class Init(
      val id: String,
    ) : Input()

    object Start : Input()

    object Stop : Input()
  }

  internal sealed class Result : Event() {
    object OnTimeTick : Result()

    data class OnTimeError(val throwable: Throwable) : Result()
  }
}
