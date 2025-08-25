package money.vivid.elmslie.samples.calculator

import kotlinx.coroutines.flow.filterIsInstance

class Calculator {

  private val plot = createPlot()

  fun digit(digit: Char) = plot.accept(Event.EnterDigit(digit))

  fun plus() = operation(Operation.PLUS)

  fun minus() = operation(Operation.MINUS)

  fun times() = operation(Operation.TIMES)

  fun divide() = operation(Operation.DIVIDE)

  private fun operation(operation: Operation) = plot.accept(Event.PerformOperation(operation))

  fun evaluate() = plot.accept(Event.Evaluate)

  fun errors() = plot.effects.filterIsInstance<Effect.NotifyError>()

  fun results() = plot.effects.filterIsInstance<Effect.NotifyNewResult>()
}
