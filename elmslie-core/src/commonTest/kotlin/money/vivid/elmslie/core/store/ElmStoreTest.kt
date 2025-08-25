package money.vivid.elmslie.core.store

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import money.vivid.elmslie.core.config.ElmslieConfig
import money.vivid.elmslie.core.plot.CoroutinesElmPlot
import money.vivid.elmslie.core.plot.ElmPlot
import money.vivid.elmslie.core.plot.ElmScheme
import money.vivid.elmslie.core.plot.NoOpPerformer
import money.vivid.elmslie.core.plot.NoOpScheme
import money.vivid.elmslie.core.plot.Performer
import money.vivid.elmslie.core.plot.SchemePart
import money.vivid.elmslie.core.plot.dsl.SchemePartBuilder
import money.vivid.elmslie.core.testutil.model.Command
import money.vivid.elmslie.core.testutil.model.Effect
import money.vivid.elmslie.core.testutil.model.Event
import money.vivid.elmslie.core.testutil.model.State

@OptIn(ExperimentalCoroutinesApi::class)
class ElmStoreTest {

    @BeforeTest
    fun beforeEach() {
        val testDispatcher = StandardTestDispatcher()
        ElmslieConfig.elmDispatcher = testDispatcher
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Should stop the store properly`() = runTest {
        val store = plot()

        store.accept(Event())
        store.stop()
        advanceUntilIdle()
    }

    @Test
    fun `Should stop getting state updates when the store is stopped`() = runTest {
        val performer =
            object : Performer<Command, Event>() {
                override fun execute(instruction: Command): Flow<Event> =
                    flow { emit(Event()) }.onEach { delay(1000) }
            }

        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    state { State(value = state.value + 1) }
                    instructions { +Command() }
                }
            },
            performer = performer,
        )

        val emittedStates = mutableListOf<State>()
        val collectJob = launch { plot.states.toList(emittedStates) }
        plot.accept(Event())
        advanceTimeBy(3500)
        plot.stop()

        assertEquals(
            mutableListOf(
                State(0), // Initial state
                State(1), // State after receiving trigger Event
                State(2), // State after executing the first command
                State(3), // State after executing the second command
                State(4), // State after executing the third command
            ),
            emittedStates,
        )
        collectJob.cancel()
    }

    @Test
    fun `Should update state when event is received`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    state { State(value = event.value) }
                }
            },
        )

        assertEquals(State(0), plot.currentState)
        plot.accept(Event(value = 10))
        advanceUntilIdle()

        assertEquals(State(10), plot.currentState)
    }

    @Test
    fun `Should not update state when it's equal to previous one`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    state { State(value = event.value) }
                }
            },
        )

        val emittedStates = mutableListOf<State>()
        val collectJob = launch { plot.states.toList(emittedStates) }

        plot.accept(Event(value = 0))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                State(0) // Initial state
            ),
            emittedStates,
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect all emitted effects`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    effects { +Effect(value = event.value) }
                }
            },
        )

        val effects = mutableListOf<Effect>()
        val collectJob = launch { plot.effects.toList(effects) }
        plot.accept(Event(value = 1))
        plot.accept(Event(value = -1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects,
        )
        collectJob.cancel()
    }

    @Test
    fun `Should skip the effect which is emitted before subscribing to effects`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    effects { +Effect(value = event.value) }
                }
            },
        )

        val effects = mutableListOf<Effect>()
        plot.accept(Event(value = 1))
        runCurrent()
        val collectJob = launch { plot.effects.toList(effects) }
        plot.accept(Event(value = -1))
        runCurrent()

        assertEquals(mutableListOf(Effect(value = -1)), effects)
        collectJob.cancel()
    }

    @Test
    fun `Should collect all effects emitted once per time`() = runTest {
        val store = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    effects {
                        +Effect(value = event.value)
                        +Effect(value = event.value)
                    }
                }
            },
        )

        val effects = mutableListOf<Effect>()
        val collectJob = launch { store.effects.toList(effects) }
        store.accept(Event(value = 1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = 1), // The second effect
            ),
            effects,
        )
        collectJob.cancel()
    }

    @Test
    fun `Should collect all emitted effects by all collectors`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    effects { +Effect(value = event.value) }
                }
            },
        )

        val effects1 = mutableListOf<Effect>()
        val effects2 = mutableListOf<Effect>()
        val collectJob1 = launch { plot.effects.toList(effects1) }
        val collectJob2 = launch { plot.effects.toList(effects2) }
        plot.accept(Event(value = 1))
        plot.accept(Event(value = -1))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects1,
        )
        assertEquals(
            mutableListOf(
                Effect(value = 1), // The first effect
                Effect(value = -1), // The second effect
            ),
            effects2,
        )
        collectJob1.cancel()
        collectJob2.cancel()
    }

    @Test
    fun `Should collect duplicated effects`() = runTest {
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    effects { +Effect(value = event.value) }
                }
            },
        )

        val effects = mutableListOf<Effect>()
        val collectJob = launch { plot.effects.toList(effects) }
        plot.accept(Event(value = 1))
        plot.accept(Event(value = 1))
        advanceUntilIdle()

        assertEquals(mutableListOf(Effect(value = 1), Effect(value = 1)), effects)
        collectJob.cancel()
    }

    @Test
    fun `Should collect event caused by actor`() = runTest {
        val performer = object : Performer<Command, Event>() {
            override fun execute(instruction: Command): Flow<Event> = flowOf(Event(instruction.value))
        }
        val plot = plot(
            scheme = object : ElmScheme<State, Event, Effect, Command>() {
                override fun SchemePartBuilder<State, Effect, Command>.reduce(
                    event: Event
                ) {
                    state { copy(value = event.value) }
                    instructions { +Command(event.value - 1).takeIf { event.value > 0 } }
                }
            },
            performer = performer,
        )

        val states = mutableListOf<State>()
        // states it a flow. TODO: Use coroutines plot
        val collectJob = launch { plot.states.toList(states) }

        plot.accept(Event(3))
        advanceUntilIdle()

        assertEquals(
            mutableListOf(
                State(0), // Initial state
                State(3), // State after receiving Event with command number
                State(2), // State after executing the first command
                State(1), // State after executing the second command
                State(0), // State after executing the third command
            ),
            states,
        )

        collectJob.cancel()
    }

    private fun plot(
        scheme: ElmScheme<State, Event, Effect, Command> = NoOpScheme(),
        performer: Performer<Command, Event> = NoOpPerformer(),
    ) = CoroutinesElmPlot(ElmPlot(scheme, performer))
}
