package money.vivid.elmslie.core.plot

import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder
import kotlin.reflect.KClass

abstract class ScreenScheme<
        State : Any,
        Event : Any,
        Ui : Any,
        Internal : Any,
        Effect : Any,
        Instruction : Any,
        >(private val uiEventClass: KClass<Ui>, private val internalEventClass: KClass<Internal>) :
    ElmScheme<State, Event, Effect, Instruction>() {

    protected abstract fun SchemePartBuilder<State, Effect, Instruction>.ui(event: Ui)

    protected abstract fun SchemePartBuilder<State, Effect, Instruction>.internal(event: Internal)

    override fun SchemePartBuilder<State, Effect, Instruction>.reduce(event: Event) {
        @Suppress("UNCHECKED_CAST")
        when {
            uiEventClass.isInstance(event) -> ui(event as Ui)
            internalEventClass.isInstance(event) -> internal(event as Internal)
            else -> error("Event ${event::class} is neither UI nor Internal")
        }
    }
}
