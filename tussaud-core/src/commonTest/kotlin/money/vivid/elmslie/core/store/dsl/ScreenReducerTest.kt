package money.vivid.elmslie.core.store.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import money.vivid.elmslie.core.plot.ScreenScheme
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

object BasicScreenScheme :
    ScreenScheme<
            TestState,
            TestScreenEvent,
            TestScreenEvent.Ui,
            TestScreenEvent.Internal,
            TestEffect,
            TestInstruction,
            >(TestScreenEvent.Ui::class, TestScreenEvent.Internal::class) {

    override fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.ui(event: TestScreenEvent.Ui) =
        when (event) {
            is TestScreenEvent.Ui.One -> state { copy(one = 1, two = 2) }
        }

    override fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.internal(event: TestScreenEvent.Internal) =
        when (event) {
            is TestScreenEvent.Internal.One ->
                instructions {
                    +TestInstruction.One
                    +TestInstruction.Two
                }
        }
}

// The same code
object PlainScreenDslScheme : ElmScheme<TestState, TestScreenEvent, TestEffect, TestInstruction>() {

    override fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.reduce(event: TestScreenEvent) =
        when (event) {
            is TestScreenEvent.Ui -> reduce(event)
            is TestScreenEvent.Internal -> reduce(event)
        }

    private fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.reduce(event: TestScreenEvent.Ui) =
        when (event) {
            is TestScreenEvent.Ui.One -> state { copy(one = 1, two = 2) }
        }

    private fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.reduce(event: TestScreenEvent.Internal) =
        when (event) {
            is TestScreenEvent.Internal.One ->
                instructions {
                    +TestInstruction.One
                    +TestInstruction.Two
                }
        }
}

internal class ScreenReducerTest {

    private val reducer = BasicScreenScheme

    @Test
    fun `Ui event is executed`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = reducer.reduce(initialState, TestScreenEvent.Ui.One)
        assertEquals(state, TestState(one = 1, two = 2))
        assertTrue(effects.isEmpty())
        assertTrue(commands.isEmpty())
    }

    @Test
    fun `Internal event is executed`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = reducer.reduce(initialState, TestScreenEvent.Internal.One)
        assertEquals(state, initialState)
        assertTrue(effects.isEmpty())
        assertEquals(commands, listOf(TestInstruction.One, TestInstruction.Two))
    }
}
