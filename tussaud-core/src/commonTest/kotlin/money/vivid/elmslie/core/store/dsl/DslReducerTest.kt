package money.vivid.elmslie.core.store.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder

private object BasicDslScheme : ElmScheme<TestState, TestEvent, TestEffect, TestInstruction>() {
    override fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.reduce(
        event: TestEvent
    ) {
        when (event) {
            is TestEvent.One -> {
                state { copy(one = 1) }
                state { copy(two = 2) }
            }

            is TestEvent.Two -> effects { +TestEffect.One }
            is TestEvent.Three ->
                operations {
                    +TestInstruction.Two
                    +TestInstruction.One
                }

            is TestEvent.Four ->
                if (event.flag) {
                    state { copy(one = 1) }
                    operations { +TestInstruction.One }
                    effects { +TestEffect.One }
                } else {
                    state { copy(one = state.two, two = state.one) }
                    effects { +TestEffect.One }
                }

            is TestEvent.Five -> applyDiff()
            is TestEvent.Six -> {
                operations { +TestInstruction.One.takeIf { event.flag } }
            }
        }
    }

    // Result editing can be done in a separate function
    private fun SchemePartBuilder<TestState, TestEffect, TestInstruction>.applyDiff() {
        state { copy(one = 0) }
        state { copy(one = initialState.one + 3) }
    }
}

internal class DslReducerTest {

    private val scheme = BasicDslScheme

    @Test
    fun `Multiple state updates are executed`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.One)
        assertEquals(state, TestState(one = 1, two = 2))
        assertTrue(effects.isEmpty())
        assertTrue(commands.isEmpty())
    }

    @Test
    fun `Effect is added`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Two)
        assertEquals(state, initialState)
        assertEquals(effects, listOf(TestEffect.One))
        assertTrue(commands.isEmpty())
    }

    @Test
    fun `Multiple commands are added`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Three)
        assertEquals(state, initialState)
        assertTrue(effects.isEmpty())
        assertEquals(commands, listOf(TestInstruction.Two, TestInstruction.One))
    }

    @Test
    fun `Complex operation`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Four(true))
        assertEquals(state, TestState(one = 1, two = 0))
        assertEquals(effects, listOf(TestEffect.One))
        assertEquals(commands, listOf(TestInstruction.One))
    }

    @Test
    fun `Condition switches state values`() {
        val initialState = TestState(one = 1, two = 2)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Four(false))
        assertEquals(state, TestState(one = 2, two = 1))
        assertEquals(effects, listOf(TestEffect.One))
        assertTrue(commands.isEmpty())
    }

    @Test
    fun `Can access initial state`() {
        val initialState = TestState(one = 1, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Five)
        assertEquals(state, TestState(one = 4, two = 0))
        assertTrue(effects.isEmpty())
        assertTrue(commands.isEmpty())
    }

    @Test
    fun `Add command conditionally`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Six(true))
        assertEquals(state, initialState)
        assertTrue(effects.isEmpty())
        assertEquals(commands, listOf(TestInstruction.One))
    }

    @Test
    fun `Skip command conditionally`() {
        val initialState = TestState(one = 0, two = 0)
        val (state, effects, commands) = scheme.reduce(initialState, TestEvent.Six(false))
        assertEquals(state, initialState)
        assertTrue(effects.isEmpty())
        assertTrue(commands.isEmpty())
    }
}
